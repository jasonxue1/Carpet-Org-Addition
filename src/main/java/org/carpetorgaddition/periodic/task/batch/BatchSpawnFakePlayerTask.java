package org.carpetorgaddition.periodic.task.batch;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.UserCache;
import net.minecraft.util.Uuids;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.CreateFakePlayerContext;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BatchSpawnFakePlayerTask extends ServerTask {
    public static final ThreadLocal<Boolean> batchSpawnHiddenMessage = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> internalBatchSpawnHiddenMessage = ThreadLocal.withInitial(() -> false);
    /**
     * 所有要召唤的玩家
     */
    private final Set<GameProfile> players = ConcurrentHashMap.newKeySet();
    private final MinecraftServer server;
    /**
     * 玩家名称的前缀
     */
    private final String prefix;
    private final int start;
    private final int end;
    /**
     * 命令的执行者
     */
    private final ServerPlayerEntity player;
    /**
     * 玩家的创建上下文，用于确定玩家上线的位置，维度，朝向等
     */
    private final CreateFakePlayerContext context;
    /**
     * 要召唤的玩家数量
     */
    private final int count;
    /**
     * 上一个游戏刻已经召唤的玩家数量
     */
    private int prevCount = 0;
    /**
     * 是否正在预加载
     */
    private boolean isPreload = true;
    private Iterator<GameProfile> iterator;
    /**
     * 是否召唤完成，用于确定任务是否结束
     */
    private boolean complete = false;
    /**
     * 任务的开始时间
     */
    private final long startTime;
    /**
     * 玩家开始创建时的时间
     */
    private long startSpawnTime = -1L;

    public BatchSpawnFakePlayerTask(MinecraftServer server, UserCache userCache, ServerPlayerEntity player, String prefix, int start, int end, Consumer<EntityPlayerMPFake> consumer) {
        this.server = server;
        this.prefix = prefix;
        this.start = start;
        this.end = end;
        int count = end - start + 1;
        this.player = player;
        this.context = new CreateFakePlayerContext(player, consumer);
        PlayerManager playerManager = server.getPlayerManager();
        for (int i = start; i <= end; i++) {
            String username = prefix + i;
            if (playerManager.getPlayer(username) != null) {
                count--;
                continue;
            }
            Thread.ofVirtual().start(() -> {
                Optional<GameProfile> optional = userCache.findByName(username);
                GameProfile gameProfile = optional.orElseGet(() -> new GameProfile(Uuids.getOfflinePlayerUuid(username), username));
                if (optional.isEmpty()) {
                    userCache.add(gameProfile);
                }
                this.players.add(gameProfile);
            });
        }
        this.count = count;
        this.startTime = FetcherUtils.getWorld(player).getTime();
    }

    @Override
    protected void tick() {
        int size = this.players.size();
        long time = FetcherUtils.getWorld(this.player).getTime();
        if (this.isPreload) {
            // 任务开始前几个游戏刻不显示进度
            boolean progress = time - this.startTime > 10;
            if (size < this.count) {
                if (progress && (this.prevCount != size || time % 40 == 0)) {
                    this.prevCount = size;
                    MutableText message = TextBuilder.translate("carpet.commands.playerManager.batch.preload", size, this.count);
                    MessageUtils.sendMessageToHud(this.player, message);
                }
                return;
            }
            if (progress) {
                MutableText message = TextBuilder.translate("carpet.commands.playerManager.batch.preload.done");
                MessageUtils.sendMessageToHud(this.player, message);
            }
            this.isPreload = false;
        }
        if (this.startSpawnTime == -1L) {
            this.startSpawnTime = System.currentTimeMillis();
        }
        // 如果召唤未能在两秒内完成，强行停止
        if (System.currentTimeMillis() - this.startSpawnTime > 2000) {
            this.timeout();
        }
        try {
            batchSpawnHiddenMessage.set(true);
            if (this.iterator == null) {
                this.iterator = this.players.iterator();
            }
            long l = System.currentTimeMillis();
            while (this.iterator.hasNext()) {
                if (System.currentTimeMillis() - l > 50) {
                    return;
                }
                GameProfile gameProfile = iterator.next();
                GenericUtils.createFakePlayer(gameProfile.getName(), this.server, this.context);
            }
        } finally {
            batchSpawnHiddenMessage.set(false);
        }
        // 显示玩家召唤者
        if (CarpetOrgAdditionSettings.displayPlayerSummoner.get()) {
            MutableText summoner = TextBuilder.translate(
                    "carpet.commands.playerManager.batch.summoner",
                    this.player.getDisplayName(),
                    this.prefix + this.start,
                    this.prefix + this.end,
                    this.count
            );
            MessageUtils.broadcastMessage(this.server, summoner);
        }
        this.complete = true;
    }

    /**
     * 任务超时，抛出异常结束任务
     */
    private void timeout() {
        ServerCommandSource source = this.player.getCommandSource();
        throw new TaskExecutionException(() -> MessageUtils.sendErrorMessage(source, "carpet.command.task.timeout"));
    }

    @Override
    protected boolean stopped() {
        return this.complete;
    }

    @Override
    public String getLogName() {
        return "玩家批量生成";
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (this == obj || this.getClass() == obj.getClass());
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
