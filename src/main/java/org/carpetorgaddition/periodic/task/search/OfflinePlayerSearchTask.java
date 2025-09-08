package org.carpetorgaddition.periodic.task.search;

import carpet.CarpetSettings;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DateTimeFormatters;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.rule.value.OpenPlayerInventory;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.wheel.*;
import org.carpetorgaddition.wheel.inventory.ImmutableInventory;
import org.carpetorgaddition.wheel.inventory.OfflinePlayerInventory;
import org.carpetorgaddition.wheel.inventory.SimulatePlayerInventory;
import org.carpetorgaddition.wheel.page.PageManager;
import org.carpetorgaddition.wheel.page.PagedCollection;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class OfflinePlayerSearchTask extends ServerTask {
    /**
     * 任务的线程池，逻辑上只能同时执行一个任务
     */
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            0,
            Runtime.getRuntime().availableProcessors() + 1,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            OfflinePlayerSearchTask::createNewThread
    );
    public static final String UNKNOWN = "[Unknown]";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatters.create();
    /**
     * 当前任务的数量
     */
    private final AtomicInteger taskCount = new AtomicInteger();
    /**
     * 已找到的物品数量
     */
    private final AtomicInteger itemCount = new AtomicInteger();
    /**
     * 在已找到的物品中，是否包含在嵌套的容器中找到的物品
     */
    private final AtomicBoolean shulkerBox = new AtomicBoolean(false);
    /**
     * 总的玩家数量
     */
    private int total = 0;
    protected final ServerCommandSource source;
    private final UserCache userCache;
    protected final ServerPlayerEntity player;
    private final MinecraftServer server;
    private final File[] files;
    private final ItemStackPredicate predicate;
    private State taksState = State.START;
    private final List<Result> results = Collections.synchronizedList(new ArrayList<>());
    /**
     * 备份文件夹的目录
     */
    private volatile WorldFormat backupFileDirectory;
    private final Object backupFileDirectoryInitLock = new Object();
    private final PagedCollection pagedCollection;

    public OfflinePlayerSearchTask(OfflinePlayerItemSearchContext context) {
        this.source = context.source();
        this.predicate = context.predicate();
        this.userCache = context.userCache();
        this.player = context.player();
        this.server = FetcherUtils.getServer(this.player);
        this.files = context.files();
        PageManager manager = FetcherUtils.getPageManager(server);
        this.pagedCollection = manager.newPagedCollection(this.source);
    }

    @Override
    protected void tick() {
        switch (this.taksState) {
            case START -> {
                for (File file : files) {
                    if (file.getName().endsWith(".dat")) {
                        UUID uuid;
                        try {
                            uuid = UUID.fromString(file.getName().split("\\.")[0]);
                        } catch (IllegalArgumentException e) {
                            // 游戏在保存玩家数据时可能产生<玩家UUID>-<随机字符串>.dat文件
                            continue;
                        }
                        this.submit(file, uuid);
                        this.total++;
                    }
                }
                this.taksState = State.RUNTIME;
            }
            case RUNTIME -> {
                if (this.taskCount.get() == 0) {
                    this.taksState = State.FEEDBACK;
                }
            }
            case FEEDBACK -> {
                this.sendFeedback();
                this.taksState = State.STOP;
            }
            case STOP -> {
            }
        }
    }

    // 创建虚拟线程
    private void submit(File unsafe, UUID uuid) {
        this.taskCount.getAndIncrement();
        EXECUTOR.submit(() -> {
            try {
                NbtCompound nbt = NbtIo.readCompressed(unsafe.toPath(), NbtSizeTracker.ofUnlimitedBytes());
                int version = NbtHelper.getDataVersion(nbt, -1);
                // 使用>=而不是==，因为存档可能降级
                if (version >= GenericUtils.getNbtDataVersion()) {
                    searchItem(uuid, nbt);
                } else {
                    this.backupAndUpdate(unsafe, uuid);
                    NbtCompound newVersionNbt = NbtIo.readCompressed(unsafe.toPath(), NbtSizeTracker.ofUnlimitedBytes());
                    searchItem(uuid, newVersionNbt);
                }
            } catch (RuntimeException | IOException e) {
                CarpetOrgAddition.LOGGER.warn("Unable to read player data from file: ", e);
            } finally {
                this.taskCount.getAndDecrement();
            }
        });
    }

    /**
     * 备份并更新玩家数据文件
     */
    private void backupAndUpdate(File unsafe, UUID uuid) {
        // 模拟玩家登录，更新玩家数据文件
        Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(uuid, this.server);
        optional.ifPresent(gameProfile -> {
            OfflinePlayerInventory inventory = new OfflinePlayerInventory(this.server, gameProfile);
            inventory.setShowLog(false);
            inventory.onOpen(this.player);
            // 备份文件
            File deletableFile = this.getBackupFileDirectory().file(unsafe.getName());
            IOUtils.copyFile(unsafe, deletableFile);
            inventory.onClose(this.player);
        });
    }

    // 查找物品
    private void searchItem(UUID uuid, NbtCompound nbt) {
        // 获取玩家配置文件
        UuidNameMappingTable table = UuidNameMappingTable.getInstance();
        Optional<GameProfile> optional = table.fetchGameProfileWithBackup(userCache, uuid);
        boolean unknownPlayer = false;
        if (optional.isEmpty()) {
            optional = Optional.of(new GameProfile(uuid, UNKNOWN));
            unknownPlayer = true;
        }
        GameProfile gameProfile = optional.get();
        // 不从在线玩家物品栏查找物品
        if (this.server.getPlayerManager().getPlayer(gameProfile.getName()) != null) {
            return;
        }
        // 统计物品栏物品
        statistics(this.getInventory(nbt), gameProfile, unknownPlayer, false);
        statistics(this.getEnderChest(nbt), gameProfile, unknownPlayer, true);
    }

    /**
     * 统计物品
     */
    private void statistics(Inventory inventory, GameProfile gameProfile, boolean unknownPlayer, boolean isEnderChest) {
        ItemStackStatistics statistics = new ItemStackStatistics(this.predicate);
        statistics.statistics(inventory);
        if (statistics.getSum() == 0) {
            return;
        }
        this.itemCount.addAndGet(statistics.getSum());
        if (statistics.hasNestingItem()) {
            this.shulkerBox.set(true);
        }
        Result result = new Result(gameProfile, statistics, unknownPlayer, isEnderChest);
        this.results.add(result);
    }

    // 获取玩家物品栏
    private Inventory getInventory(NbtCompound nbt) {
        return SimulatePlayerInventory.of(nbt, FetcherUtils.getServer(this.player));
    }

    /**
     * 从NBT读取玩家末影箱
     *
     * @see PlayerEntity#readCustomDataFromNbt(NbtCompound)
     */
    @SuppressWarnings("JavadocReference")
    protected Inventory getEnderChest(NbtCompound nbt) {
        EnderChestInventory inventory = new EnderChestInventory();
        if (nbt.contains("EnderItems", NbtElement.LIST_TYPE)) {
            inventory.readNbtList(nbt.getList("EnderItems", NbtElement.COMPOUND_TYPE), this.player.getRegistryManager());
            return inventory;
        } else {
            return ImmutableInventory.EMPTY;
        }
    }

    // 发送命令反馈
    private void sendFeedback() {
        if (this.results.isEmpty()) {
            MessageUtils.sendMessage(
                    this.source,
                    "carpet.commands.finder.item.offline_player.not_found",
                    this.getContainerName(false),
                    this.predicate.toText()
            );
            return;
        }
        int resultCount = this.results.size();
        this.results.sort((o1, o2) -> o2.statistics().getSum() - o1.statistics().getSum());
        this.pagedCollection.addContent(this.results);
        Text itemCount = getItemCount();
        Text numberOfPeople = getNumberOfPeople(resultCount);
        MutableText message = getFirstFeedback(numberOfPeople, itemCount);
        TextBuilder builder = new TextBuilder(message);
        builder.setHover("carpet.commands.finder.item.offline_player.prompt", this.getContainerName(false));
        MessageUtils.sendEmptyMessage(this.source);
        MessageUtils.sendMessage(this.source, builder.build());
        CommandUtils.handlingException(this.pagedCollection::print, source);
    }

    /**
     * 获取首条反馈消息
     */
    private MutableText getFirstFeedback(Text numberOfPeople, Text itemCount) {
        return TextBuilder.translate(
                "carpet.commands.finder.item.offline_player",
                numberOfPeople,
                this.getContainerName(false),
                itemCount,
                this.predicate.toText()
        );
    }

    /**
     * 获取物品数量文本
     */
    private Text getItemCount() {
        if (this.predicate.isConvertible()) {
            return FinderCommand.showCount(this.predicate.asItem().getDefaultStack(), this.itemCount.get(), this.shulkerBox.get());
        } else {
            TextBuilder builder = new TextBuilder(this.itemCount);
            return this.shulkerBox.get() ? builder.setItalic().build() : builder.build();
        }
    }

    /**
     * 获取玩家数量文本
     */
    private Text getNumberOfPeople(int resultCount) {
        // 玩家总数的悬停提示
        ArrayList<Text> peopleHover = new ArrayList<>();
        peopleHover.add(TextBuilder.translate("carpet.commands.finder.item.offline_player.total", this.total));
        peopleHover.add(TextBuilder.translate("carpet.commands.finder.item.offline_player.found", resultCount));
        TextBuilder builder = new TextBuilder(resultCount);
        builder.setHover(TextBuilder.joinList(peopleHover));
        // 玩家总数文本
        return builder.build();
    }

    private Text getContainerName(boolean isEnderChest) {
        return isEnderChest
                ? TextBuilder.translate("carpet.commands.finder.item.offline_player.container.enderchest")
                : TextBuilder.translate("carpet.commands.finder.item.offline_player.container.inventory");
    }

    @Override
    protected boolean stopped() {
        return this.taksState == State.STOP;
    }

    @Override
    public String getLogName() {
        return "从离线玩家物品栏寻找物品";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || this.getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    /**
     * 添加打开玩家物品栏按钮
     */
    @Nullable
    private Text openInventoryButton(GameProfile gameProfile) {
        if (CommandUtils.canUseCommand(source, CarpetSettings.commandPlayer) && OpenPlayerInventory.isEnable(source)) {
            String command = CommandProvider.openPlayerInventory(gameProfile.getId());
            TextBuilder builder = new TextBuilder("[O]");
            builder.setCommand(command);
            builder.setHover("carpet.commands.finder.item.offline_player.open.inventory");
            builder.setColor(Formatting.GRAY);
            return builder.build();
        }
        return null;
    }

    @Nullable
    private Text openEnderChestButton(GameProfile gameProfile) {
        if (CommandUtils.canUseCommand(this.source, CarpetSettings.commandPlayer) && OpenPlayerInventory.isEnable(this.source)) {
            String command = CommandProvider.openPlayerEnderChest(gameProfile.getId());
            MutableText clickLogin = TextBuilder.translate("carpet.commands.finder.item.offline_player.open.ender_chest");
            TextBuilder builder = new TextBuilder("[O]");
            builder.setColor(Formatting.GRAY);
            builder.setHover(clickLogin);
            builder.setCommand(command);
            return builder.build();
        }
        return null;
    }

    /**
     * @return 获取备份目录
     */
    private WorldFormat getBackupFileDirectory() {
        if (this.backupFileDirectory == null) {
            synchronized (this.backupFileDirectoryInitLock) {
                String time = LocalDateTime.now().format(FORMATTER) + "_" + GenericUtils.getNbtDataVersion();
                this.backupFileDirectory = new WorldFormat(this.server, "backups", "playerdata", time);
            }
        }
        return this.backupFileDirectory;
    }

    private static Thread createNewThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.setName(OfflinePlayerSearchTask.class.getSimpleName() + " - Thread");
        return thread;
    }

    /**
     * @apiNote 非静态的内部类强引用了外部类导致暂时无法被回收，但这不是问题
     */
    public class Result implements Supplier<Text> {
        private final GameProfile gameProfile;
        private final ItemStackStatistics statistics;
        private final boolean isUnknown;
        private final boolean isEnderChest;

        private Result(GameProfile gameProfile, ItemStackStatistics statistics, boolean isUnknown, boolean isEnderChest) {
            this.gameProfile = gameProfile;
            this.statistics = statistics;
            this.isUnknown = isUnknown;
            this.isEnderChest = isEnderChest;
        }

        @Override
        public Text get() {
            // 获取玩家名，并添加UUID悬停提示
            String name = gameProfile.getName();
            String uuid = gameProfile().getId().toString();
            // 悬停提示
            Text hover = TextBuilder.combineAll("UUID: %s\n".formatted(uuid), TextProvider.COPY_CLICK);
            // 获取物品数量，如果包含在潜影盒中找到的物品，就设置物品为斜体
            Text count = statistics().getCountText();
            TextBuilder builder = getDisplayPlayerName(name, uuid, hover, count, this.isEnderChest);
            return builder.build();
        }

        // 获取玩家显示名称
        private TextBuilder getDisplayPlayerName(String name, String uuid, Text hover, Text count, boolean isEnderChest) {
            boolean unknown = isUnknown();
            TextBuilder builder = new TextBuilder(unknown ? name : "[" + name + "]");
            if (unknown) {
                builder.setStrikethrough()
                        .setCopyToClipboard(uuid, false)
                        .append(createSearchButton());
            } else {
                builder.append(createLoginButton())
                        .setCopyToClipboard(name, false);
            }
            Text button = isEnderChest ? openEnderChestButton(this.gameProfile()) : openInventoryButton(this.gameProfile());
            builder.append(button)
                    .setHover(hover)
                    .setColor(Formatting.GRAY);
            Text container = getContainerName(isEnderChest);
            return TextBuilder.of("carpet.commands.finder.item.offline_player.each", builder.build(), container, count);
        }

        // 创建单击上线按钮
        private Text createLoginButton() {
            if (CommandUtils.canUseCommand(source, CarpetSettings.commandPlayer)) {
                String command = CommandProvider.spawnFakePlayer(gameProfile().getName());
                TextBuilder builder = new TextBuilder(" [↑]");
                builder.setCommand(command);
                builder.setHover("carpet.command.text.click.login");
                return builder.build();
            }
            return TextBuilder.empty();
        }

        // 创建查询玩家名称按钮
        private TextBuilder createSearchButton() {
            // 按钮的悬停提示
            ArrayList<Text> list = new ArrayList<>();
            list.add(TextBuilder.translate("carpet.commands.finder.item.offline_player.query.name"));
            list.add(TextBuilder.of("carpet.commands.finder.item.offline_player.query.non_authentic").setColor(Formatting.RED).build());
            TextBuilder button = new TextBuilder(" [\uD83D\uDD0D]");
            // 设置单击按钮执行命令
            button.setCommand(CommandProvider.queryPlayerName(gameProfile().getId()));
            // 设置按钮悬停提示
            button.setHover(TextBuilder.joinList(list));
            return button;
        }

        public GameProfile gameProfile() {
            return gameProfile;
        }

        public ItemStackStatistics statistics() {
            return statistics;
        }

        public boolean isUnknown() {
            return isUnknown;
        }
    }

    public enum State {
        START,
        RUNTIME,
        FEEDBACK,
        STOP
    }
}
