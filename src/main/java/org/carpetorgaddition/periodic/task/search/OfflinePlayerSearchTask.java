package org.carpetorgaddition.periodic.task.search;

import carpet.CarpetSettings;
import carpet.utils.CommandHelper;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.DataFixer;
import net.minecraft.datafixer.DataFixTypes;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.rule.value.OpenPlayerInventory;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.util.inventory.SimulatePlayerInventory;
import org.carpetorgaddition.util.page.PageManager;
import org.carpetorgaddition.util.page.PagedCollection;
import org.carpetorgaddition.util.provider.CommandProvider;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.carpetorgaddition.util.wheel.ItemStackStatistics;
import org.carpetorgaddition.util.wheel.UuidNameMappingTable;
import org.carpetorgaddition.util.wheel.WorldFormat;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
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
     * 已忽略的玩家数量
     */
    private final AtomicInteger ignoreCount = new AtomicInteger();
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
    private final WorldFormat tempFileDirectory;
    private final PagedCollection pagedCollection;

    public OfflinePlayerSearchTask(OfflinePlayerItemSearchContext context) {
        this.source = context.source();
        this.predicate = context.predicate();
        this.userCache = context.userCache();
        this.player = context.player();
        this.server = this.player.server;
        this.files = context.files();
        this.showUnknown = context.showUnknown();
        this.tempFileDirectory = new WorldFormat(this.server, "temp", "playerdata");
        PageManager manager = GenericFetcherUtils.getPageManager(server);
        this.pagedCollection = manager.newPagedCollection(this.source);
    }

    @Override
    protected void tick() {
        switch (this.taksState) {
            case START -> {
                for (File file : files) {
                    if (file.getName().endsWith(".dat")) {
                        this.createVirtualThread(file);
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
    private void createVirtualThread(File unsafe) {
        this.threadCount.getAndIncrement();
        Thread.ofVirtual().start(() -> {
            try {
                NbtCompound maybeOldNbt = NbtIo.readCompressed(unsafe.toPath(), NbtSizeTracker.ofUnlimitedBytes());
                int version = NbtHelper.getDataVersion(maybeOldNbt, -1);
                // 使用>=而不是==，因为存档可能降级
                if (version >= GameUtils.getNbtDataVersion()) {
                    searchItem(unsafe, maybeOldNbt, version, false);
                } else {
                    // NBT的数据版本与当前游戏的数据版本不匹配，先复制再读取复制的文件，避免对源文件产生影响
                    File deletableFile = this.tempFileDirectory.file(unsafe.getName());
                    // 复制文件，避免影响源文件
                    IOUtils.copyFile(unsafe, deletableFile);
                    searchItem(deletableFile, maybeOldNbt, version, true);
                    // 删除临时文件
                    if (deletableFile.delete()) {
                        return;
                    }
                    CarpetOrgAddition.LOGGER.warn("未成功删除临时文件{}", deletableFile.getName());
                }
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.warn("无法从文件读取玩家数据：", e);
            } finally {
                this.threadCount.getAndDecrement();
            }
        });
    }

    // 查找物品
    private void searchItem(File file, NbtCompound maybeOldNbt, int version, boolean needToUpgrade) {
        String uuidString = file.getName().split("\\.")[0];
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            this.ignoreCount.getAndIncrement();
            CarpetOrgAddition.LOGGER.warn("无法从文件名解析UUID，正在忽略文件：{}", file.getName());
            return;
        }
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
        // 从玩家NBT读取物品栏
        DataFixer dataFixer = this.server.getDataFixer();
        // 看起来这个方法并没有将新的NBT重新写入文件，这是否意味着它不会对源文件产生影响？
        NbtCompound nbt = needToUpgrade ? DataFixTypes.PLAYER.update(dataFixer, maybeOldNbt, version) : maybeOldNbt;
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
        return SimulatePlayerInventory.of(nbt, this.player.getServer());
    }

    // 发送命令反馈
    private void sendFeedback() {
        if (this.list.isEmpty()) {
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
        MutableText hoverPrompt = TextUtils.translate(
                "carpet.commands.finder.item.offline_player.prompt",
                this.getInventoryName()
        );
        Text itemCount = getItemCount();
        Text numberOfPeople = getNumberOfPeople(resultCount);
        MutableText message = getFirstFeedback(numberOfPeople, itemCount);
        MessageUtils.sendMessage(this.source, TextUtils.hoverText(message, hoverPrompt));
        int skip = this.skipCount.get();
        if (skip != 0) {
            // 如果this.unknownPlayer为true，那么代码不应该执行到这里
            MutableText translate = TextUtils.translate("carpet.commands.finder.item.offline_player.skip", skip);
            MutableText grayItalic = TextUtils.toGrayItalic(translate);
            MessageUtils.sendMessage(this.source, grayItalic);
        }
        CommandUtils.handlingException(this.pagedCollection::print, source);
    }

    /**
     * 获取首条反馈消息
     */
    private MutableText getFirstFeedback(Text numberOfPeople, Text itemCount) {
        return TextUtils.translate(
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
            MutableText text = TextUtils.createText(Integer.toString(this.itemCount.get()));
            return this.shulkerBox.get() ? TextUtils.toItalic(text) : text;
        }
    }

    /**
     * 获取玩家数量文本
     */
    private Text getNumberOfPeople(int resultCount) {
        // 玩家总数的悬停提示
        ArrayList<Text> peopleHover = new ArrayList<>();
        peopleHover.add(TextUtils.translate("carpet.commands.finder.item.offline_player.total", this.total - this.ignoreCount.get()));
        peopleHover.add(TextUtils.translate("carpet.commands.finder.item.offline_player.found", resultCount));
        if (!this.showUnknown) {
            peopleHover.add(TextUtils.translate("carpet.commands.finder.item.offline_player.skipped", this.skipCount.get()));
        }
        // 玩家总数文本
        return TextUtils.hoverText(TextUtils.createText(resultCount), TextUtils.appendList(peopleHover));
    }

    protected Text getInventoryName() {
        return TextUtils.translate("container.inventory");
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
        if (CommandHelper.canUseCommand(source, CarpetSettings.commandPlayer) && OpenPlayerInventory.isEnable(source)) {
            String command = CommandProvider.openPlayerInventory(gameProfile.getId());
            MutableText clickLogin = TextUtils.translate("carpet.commands.finder.item.offline_player.open.inventory");
            return TextUtils.command(TextUtils.createText("[O]"), command, clickLogin, Formatting.GRAY);
        }
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(player);
    }

    /**
     * @apiNote 非静态的内部类强引用了外部类导致暂时无法被回收，但这不是问题
     */
    private class Result implements Supplier<Text> {
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
            Text hover = TextUtils.appendAll(TextUtils.createText("UUID: %s\n".formatted(uuid)), TextProvider.COPY_CLICK);
            // 获取物品数量，如果包含在潜影盒中找到的物品，就设置物品为斜体
            Text count = statistics().getCountText();
            MutableText playerName;
            if (isUnknown()) {
                // 单击复制玩家UUID
                playerName = TextUtils.appendAll(
                        TextUtils.copy(name, uuid, hover, Formatting.GRAY),
                        " ",
                        openInventoryButton(this.gameProfile)
                );
            } else {
                // 添加单击上线按钮
                Text loginButton;
                if (CommandHelper.canUseCommand(source, CarpetSettings.commandPlayer)) {
                    String command = CommandProvider.spawnFakePlayer(gameProfile().getName());
                    MutableText clickLogin = TextUtils.translate("carpet.command.text.click.login");
                    loginButton = TextUtils.command(TextUtils.createText(" [↑]"), command, clickLogin, Formatting.GRAY);
                } else {
                    loginButton = null;
                }
                // 单击复制玩家名
                playerName = TextUtils.appendAll(
                        TextUtils.copy("[" + name + "]", name, hover, Formatting.GRAY),
                        loginButton,
                        openInventoryButton(this.gameProfile)
                );
            }
            MutableText translate = TextUtils.translate(
                    "carpet.commands.finder.item.offline_player.each",
                    playerName,
                    getInventoryName(),
                    count
            );
            if (isUnknown()) {
                // 添加搜索按钮
                MutableText button = TextUtils.appendAll(
                        TextUtils.translate("carpet.commands.finder.item.offline_player.query.name"), "\n",
                        TextUtils.setColor(
                                TextUtils.translate("carpet.commands.finder.item.offline_player.query.non_authentic"),
                                Formatting.RED
                        ));
                MutableText command = TextUtils.command(
                        // 一个Unicode字符集中的放大镜字符，这在原版游戏中看起来并没有什么问题
                        // 但是在其他字体（例如服务器控制台的字体）下会显得有些格格不入，这还是首次在游戏中使用Emoji字符
                        TextUtils.createText("[\uD83D\uDD0D]"),
                        CommandProvider.queryPlayerName(gameProfile().getId()),
                        button, Formatting.AQUA, false
                );
                translate = TextUtils.appendAll(TextUtils.toStrikethrough(translate), " ", command);
            }
            return translate;
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

    private enum State {
        START,
        RUNTIME,
        FEEDBACK,
        STOP
    }
}
