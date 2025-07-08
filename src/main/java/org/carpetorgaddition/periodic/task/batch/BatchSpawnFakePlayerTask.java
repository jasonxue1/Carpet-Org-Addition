package org.carpetorgaddition.periodic.task.batch;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.UserCache;
import net.minecraft.util.Uuids;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.CreateFakePlayerContext;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BatchSpawnFakePlayerTask extends ServerTask {
    public static final ThreadLocal<Boolean> batchSpawnHiddenMessage = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> internalBatchSpawnHiddenMessage = ThreadLocal.withInitial(() -> false);
    private final Set<GameProfile> players = ConcurrentHashMap.newKeySet();
    private final MinecraftServer server;
    private final String prefix;
    private final int start;
    private final int end;
    private final ServerPlayerEntity player;
    private final CreateFakePlayerContext context;
    private final int count;
    private int prevCount = 0;
    private boolean complete = false;
    private final long startTime;

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
            CompletableFuture<Optional<GameProfile>> future = userCache.findByNameAsync(username);
            future.thenAccept(optional -> {
                GameProfile gameProfile = optional.orElseGet(() -> new GameProfile(Uuids.getOfflinePlayerUuid(username), username));
                this.players.add(gameProfile);
            });
        }
        this.count = count;
        this.startTime = player.getWorld().getTime();
    }

    @Override
    protected void tick() {
        int size = this.players.size();
        long time = this.player.getWorld().getTime();
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
        try {
            batchSpawnHiddenMessage.set(true);
            for (GameProfile gameProfile : this.players) {
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
