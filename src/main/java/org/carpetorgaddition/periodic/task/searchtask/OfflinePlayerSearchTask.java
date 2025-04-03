package org.carpetorgaddition.periodic.task.searchtask;

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
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.GameUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.SimulatePlayerInventory;
import org.carpetorgaddition.util.provider.CommandProvider;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.carpetorgaddition.util.wheel.ItemStackStatistics;
import org.carpetorgaddition.util.wheel.UuidNameMappingTable;
import org.carpetorgaddition.util.wheel.WorldFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class OfflinePlayerSearchTask extends ServerTask {
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
    private final ServerCommandSource source;
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

    public OfflinePlayerSearchTask(OfflinePlayerItemSearchContext context) {
        this.source = context.source();
        this.predicate = context.predicate();
        this.userCache = context.userCache();
        this.player = context.player();
        this.server = this.player.server;
        this.files = context.files();
        this.showUnknown = context.showUnknown();
        this.tempFileDirectory = new WorldFormat(this.server, "temp", "playerdata");
    }

    @Override
    protected void tick() {
        switch (this.taksState) {
            case START -> {
                for (File file : files) {
                    if (file.getName().endsWith(".dat")) {
                        createVirtualThread(file);
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
                if (version == GameUtils.getNbtDataVersion()) {
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
            CarpetOrgAddition.LOGGER.warn("无法从文件名解析UUID，正在忽略文件：{}", file.getName());
            return;
        }
        // 获取玩家配置文件
        UuidNameMappingTable table = UuidNameMappingTable.getInstance();
        Optional<GameProfile> optional = table.fetchGameProfileWithBackup(userCache, uuid);
        boolean unknownPlayer = false;
        if (optional.isEmpty()) {
            if (this.showUnknown) {
                optional = Optional.of(new GameProfile(uuid, "[Unknown]"));
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
        this.list.sort((o1, o2) -> o2.statistics().getSum() - o1.statistics().getSum());
        MutableText hoverPrompt = TextUtils.translate(
                "carpet.commands.finder.item.offline_player.prompt",
                this.getInventoryName()
        );
        MutableText message;
        Text count;
        if (this.predicate.canConvertItem()) {
            count = FinderCommand.showCount(this.predicate.asItem().getDefaultStack(), this.itemCount.get(), this.shulkerBox.get());
        } else {
            MutableText text = TextUtils.createText(Integer.toString(this.itemCount.get()));
            if (this.shulkerBox.get()) {
                text = TextUtils.toItalic(text);
            }
            count = text;
        }
        if (this.list.size() > CarpetOrgAdditionSettings.finderCommandMaxFeedbackCount) {
            message = TextUtils.translate(
                    "carpet.commands.finder.item.offline_player.limit",
                    this.list.size(),
                    this.getInventoryName(),
                    count,
                    this.predicate.toText(),
                    CarpetOrgAdditionSettings.finderCommandMaxFeedbackCount
            );
        } else {
            message = TextUtils.translate(
                    "carpet.commands.finder.item.offline_player",
                    this.list.size(),
                    this.getInventoryName(),
                    count,
                    this.predicate.toText()
            );
        }
        MessageUtils.sendMessage(this.source, TextUtils.hoverText(message, hoverPrompt));
        int skip = this.skipCount.get();
        if (skip != 0) {
            // 如果this.unknownPlayer为true，那么代码不应该执行到这里
            MutableText translate = TextUtils.translate("carpet.commands.finder.item.offline_player.skip", skip);
            MutableText grayItalic = TextUtils.toGrayItalic(translate);
            MessageUtils.sendMessage(this.source, grayItalic);
        }
        for (int i = 0; i < Math.min(this.list.size(), CarpetOrgAdditionSettings.finderCommandMaxFeedbackCount); i++) {
            Result result = this.list.get(i);
            sendEveryFeedback(result);
        }
    }

    // 发送每一条反馈
    private void sendEveryFeedback(Result result) {
        // TODO 能找到名称的玩家单击复制玩家名，否则复制UUID
        // 获取玩家名，并添加UUID悬停提示
        String name = result.gameProfile.getName();
        String uuid = result.gameProfile().getId().toString();
        // 悬停提示
        Text hover = TextUtils.appendAll(TextUtils.createText("UUID: %s\n".formatted(uuid)), TextUtils.translate("chat.copy.click"));
        // 获取物品数量，如果包含在潜影盒中找到的物品，就设置物品为斜体
        Text count = result.statistics().getCountText();
        MutableText playerName;
        if (result.isUnknown()) {
            playerName = TextUtils.copy(name, name, hover, Formatting.GRAY);
        } else {
            // 添加单击上线按钮
            String command = CommandProvider.spawnFakePlayer(result.gameProfile().getName());
            MutableText clickLogin = TextUtils.translate("carpet.command.text.click.login");
            Text button = TextUtils.command(TextUtils.createText("[↑]"), command, clickLogin, Formatting.AQUA);
            playerName = TextUtils.appendAll(TextUtils.copy(name, uuid, hover, Formatting.GRAY), button);
        }
        MutableText translate = TextUtils.translate(
                "carpet.commands.finder.item.offline_player.each",
                playerName,
                this.getInventoryName(),
                count
        );
        if (result.isUnknown()) {
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
                    CommandProvider.queryPlayerName(result.gameProfile().getId()),
                    button, Formatting.AQUA, false
            );
            translate = TextUtils.appendAll(TextUtils.toStrikethrough(translate), " ", command);
        }
        MessageUtils.sendMessage(this.source, translate);
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

    @Override
    public int hashCode() {
        return Objects.hashCode(player);
    }

    private record Result(GameProfile gameProfile, ItemStackStatistics statistics, boolean isUnknown) {
    }

    private enum State {
        START,
        RUNTIME,
        FEEDBACK,
        STOP
    }
}
