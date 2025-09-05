package org.carpetorgaddition.periodic.task.search;

import carpet.CarpetSettings;
import com.mojang.authlib.GameProfile;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
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
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class OfflinePlayerSearchTask extends ServerTask {
    public static final String UNKNOWN = "[Unknown]";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatters.create();
    /**
     * 当前虚拟线程的数量
     */
    private final AtomicInteger threadCount = new AtomicInteger();
    /**
     * 已找到的物品数量
     */
    private final AtomicInteger itemCount = new AtomicInteger();
    /**
     * 被跳过的玩家数量
     */
    private final AtomicInteger skipCount = new AtomicInteger();
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
    private final boolean showUnknown;
    private State taksState = State.START;
    // synchronized会导致虚拟线程被锁定吗？还能不能使用并发集合？
    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayList<Result> list = new ArrayList<>();
    /**
     * 备份文件夹的目录
     */
    private volatile WorldFormat backupFileDirectory;
    private final ReentrantLock backupFileDirectoryInitLock = new ReentrantLock();
    private final PagedCollection pagedCollection;

    public OfflinePlayerSearchTask(OfflinePlayerItemSearchContext context) {
        this.source = context.source();
        this.predicate = context.predicate();
        this.userCache = context.userCache();
        this.player = context.player();
        this.server = this.player.getWorld().getServer();
        this.files = context.files();
        this.showUnknown = context.showUnknown();
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
                        // TODO 改为线程池
                        this.createVirtualThread(file, uuid);
                        this.total++;
                    }
                }
                this.taksState = State.RUNTIME;
            }
            case RUNTIME -> {
                if (this.threadCount.get() == 0) {
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
    private void createVirtualThread(File unsafe, UUID uuid) {
        this.threadCount.getAndIncrement();
        Thread.ofVirtual().start(() -> {
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
                this.threadCount.getAndDecrement();
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
            if (this.showUnknown) {
                optional = Optional.of(new GameProfile(uuid, UNKNOWN));
                unknownPlayer = true;
            } else {
                this.skipCount.incrementAndGet();
                return;
            }
        }
        GameProfile gameProfile = optional.get();
        // 不从在线玩家物品栏查找物品
        if (this.server.getPlayerManager().getPlayer(gameProfile.getName()) != null) {
            return;
        }
        // 统计物品栏物品
        Inventory inventory = getInventory(nbt);
        ItemStackStatistics statistics = new ItemStackStatistics(this.predicate);
        statistics.statistics(inventory);
        if (statistics.getSum() == 0) {
            return;
        }
        this.itemCount.addAndGet(statistics.getSum());
        if (statistics.hasNestingItem()) {
            this.shulkerBox.set(true);
        }
        Result result = new Result(gameProfile, statistics, unknownPlayer);
        try {
            this.lock.lock();
            this.list.add(result);
        } finally {
            this.lock.unlock();
        }
    }

    // 获取玩家物品栏
    protected Inventory getInventory(NbtCompound nbt) {
        return SimulatePlayerInventory.of(nbt, FetcherUtils.getServer(this.player));
    }

    // 发送命令反馈
    private void sendFeedback() {
        if (this.list.isEmpty()) {
            this.sendSkipPlayerMessage();
            MessageUtils.sendMessage(
                    this.source,
                    "carpet.commands.finder.item.offline_player.not_found",
                    this.getInventoryName(),
                    this.predicate.toText()
            );
            return;
        }
        int resultCount = this.list.size();
        this.list.sort((o1, o2) -> o2.statistics().getSum() - o1.statistics().getSum());
        this.pagedCollection.addContent(this.list);
        Text itemCount = getItemCount();
        Text numberOfPeople = getNumberOfPeople(resultCount);
        MutableText message = getFirstFeedback(numberOfPeople, itemCount);
        TextBuilder builder = new TextBuilder(message);
        builder.setHover("carpet.commands.finder.item.offline_player.prompt", this.getInventoryName());
        MessageUtils.sendEmptyMessage(this.source);
        this.sendSkipPlayerMessage();
        MessageUtils.sendMessage(this.source, builder.build());
        CommandUtils.handlingException(this.pagedCollection::print, source);
    }

    private void sendSkipPlayerMessage() {
        int skip = this.skipCount.get();
        if (skip != 0) {
            // 如果this.unknownPlayer为true，那么代码不应该执行到这里
            TextBuilder prompt = TextBuilder.of("carpet.commands.finder.item.offline_player.skip", skip);
            prompt.setGrayItalic();
            MessageUtils.sendMessage(this.source, prompt.build());
        }
    }

    /**
     * 获取首条反馈消息
     */
    private MutableText getFirstFeedback(Text numberOfPeople, Text itemCount) {
        return TextBuilder.translate(
                "carpet.commands.finder.item.offline_player",
                numberOfPeople,
                this.getInventoryName(),
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
        if (!this.showUnknown) {
            peopleHover.add(TextBuilder.translate("carpet.commands.finder.item.offline_player.skipped", this.skipCount.get()));
        }
        TextBuilder builder = new TextBuilder(resultCount);
        builder.setHover(TextBuilder.joinList(peopleHover));
        // 玩家总数文本
        return builder.build();
    }

    protected Text getInventoryName() {
        return TextBuilder.translate("carpet.commands.finder.item.offline_player.container.inventory");
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
        if (getClass() == o.getClass()) {
            return Objects.equals(player, ((OfflinePlayerSearchTask) o).player);
        }
        return false;
    }

    /**
     * 添加打开玩家物品栏按钮
     */
    @Nullable
    protected Text openInventoryButton(GameProfile gameProfile) {
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

    @Override
    public int hashCode() {
        return Objects.hashCode(player);
    }

    /**
     * @return 获取备份目录
     */
    private WorldFormat getBackupFileDirectory() {
        if (this.backupFileDirectory == null) {
            try {
                this.backupFileDirectoryInitLock.lock();
                if (this.backupFileDirectory == null) {
                    String time = LocalDateTime.now().format(FORMATTER) + "_" + GenericUtils.getNbtDataVersion();
                    this.backupFileDirectory = new WorldFormat(this.server, "backups", "playerdata", time);
                }
            } finally {
                this.backupFileDirectoryInitLock.unlock();
            }
        }
        return this.backupFileDirectory;
    }

    /**
     * @apiNote 非静态的内部类强引用了外部类导致暂时无法被回收，但这不是问题
     */
    public class Result implements Supplier<Text> {
        private final GameProfile gameProfile;
        private final ItemStackStatistics statistics;
        private final boolean isUnknown;

        private Result(GameProfile gameProfile, ItemStackStatistics statistics, boolean isUnknown) {
            this.gameProfile = gameProfile;
            this.statistics = statistics;
            this.isUnknown = isUnknown;
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
            TextBuilder builder = getDisplayPlayerName(name, uuid, hover, count);
            if (isUnknown()) {
                // 设置删除线
                builder.setStrikethrough();
                addSearchButton(builder);
            }
            return builder.build();
        }

        // 添加搜索按钮
        private void addSearchButton(TextBuilder builder) {
            // 按钮的悬停提示
            ArrayList<Text> list = new ArrayList<>();
            list.add(TextBuilder.translate("carpet.commands.finder.item.offline_player.query.name"));
            list.add(TextBuilder.of("carpet.commands.finder.item.offline_player.query.non_authentic").setColor(Formatting.RED).build());
            TextBuilder button = new TextBuilder(" [\uD83D\uDD0D]");
            // 设置单击按钮执行命令
            button.setCommand(CommandProvider.queryPlayerName(gameProfile().getId()));
            // 设置按钮悬停提示
            button.setHover(TextBuilder.joinList(list));
            // 设置按钮颜色
            button.setColor(Formatting.AQUA);
            builder.append(button);
        }

        // 获取玩家显示名称
        private TextBuilder getDisplayPlayerName(String name, String uuid, Text hover, Text count) {
            TextBuilder builder;
            if (isUnknown()) {
                // 单击复制玩家UUID
                builder = new TextBuilder(name)
                        .setCopyToClipboard(uuid)
                        .setHover(hover)
                        .setColor(Formatting.GRAY)
                        .append(" ")
                        .append(openInventoryButton(this.gameProfile));
            } else {
                // 单击复制玩家名
                builder = new TextBuilder("[" + name + "]")
                        .setCopyToClipboard(name)
                        .setHover(hover)
                        .setColor(Formatting.GRAY)
                        .append(createLoginButton())
                        .append(openInventoryButton(this.gameProfile));
            }
            return TextBuilder.of("carpet.commands.finder.item.offline_player.each", builder.build(), getInventoryName(), count);
        }

        // 添加单击上线按钮
        private Text createLoginButton() {
            if (CommandUtils.canUseCommand(source, CarpetSettings.commandPlayer)) {
                String command = CommandProvider.spawnFakePlayer(gameProfile().getName());
                TextBuilder builder = new TextBuilder(" [↑]");
                builder.setColor(Formatting.GRAY);
                builder.setCommand(command);
                builder.setHover("carpet.command.text.click.login");
                return builder.build();
            }
            return TextBuilder.empty();
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
