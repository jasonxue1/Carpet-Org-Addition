package boat.carpetorgaddition.periodic.task.batch;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.periodic.task.ServerTask;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.FakePlayerCreateContext;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserNameToIdResolver;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BatchSpawnFakePlayerTask extends ServerTask {
    public static final ThreadLocal<Boolean> batchSpawnHiddenMessage = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> internalBatchSpawnHiddenMessage = ThreadLocal.withInitial(() -> false);
    /**
     * 所有要召唤的玩家
     */
    private final Set<NameAndId> players = ConcurrentHashMap.newKeySet();
    private final MinecraftServer server;
    /**
     * 玩家名称的前缀
     */
    private final String prefix;
    private final int start;
    private final int end;
    /**
     * 玩家的创建上下文，用于确定玩家上线的位置，维度，朝向等
     */
    private final FakePlayerCreateContext context;
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
    private Iterator<NameAndId> iterator;
    /**
     * 是否召唤完成，用于确定任务是否结束
     */
    private boolean complete = false;
    /**
     * 任务的开始时间
     */
    private final long startTime;
    /**
     * 玩家档案预加载使用的时间
     */
    private long setupTime = -1L;
    public static final LocalizationKey KEY = PlayerManagerCommand.KEY.then("batch");

    public BatchSpawnFakePlayerTask(MinecraftServer server, CommandSourceStack source, UserNameToIdResolver userCache, FakePlayerCreateContext context, String prefix, int start, int end) {
        super(source);
        this.server = server;
        this.prefix = prefix;
        this.start = start;
        this.end = end;
        int count = end - start + 1;
        this.context = context;
        PlayerList playerManager = server.getPlayerList();
        for (int i = start; i <= end; i++) {
            String username = prefix + i;
            if (playerManager.getPlayerByName(username) != null) {
                count--;
                continue;
            }
            Thread.ofVirtual().start(() -> {
                Optional<NameAndId> optional = userCache.get(username);
                NameAndId gameProfile = optional.orElseGet(() -> new NameAndId(UUIDUtil.createOfflinePlayerUUID(username), username));
                if (optional.isEmpty()) {
                    userCache.add(gameProfile);
                }
                this.players.add(gameProfile);
            });
        }
        this.count = count;
        this.startTime = ServerUtils.getWorld(this.source).getGameTime();
    }

    @Override
    protected void tick() {
        int size = this.players.size();
        long time = ServerUtils.getWorld(this.source).getGameTime();
        if (this.isPreload) {
            // 任务开始前几个游戏刻不显示进度
            boolean progress = time - this.startTime > 10;
            LocalizationKey key = KEY.then("preload");
            if (size < this.count) {
                if (progress && (this.prevCount != size || time % 40 == 0)) {
                    this.prevCount = size;
                    MessageUtils.sendMessageToHudIfPlayer(this.source, () -> key.translate(size, this.count));
                }
                return;
            }
            if (progress) {
                MessageUtils.sendMessageToHudIfPlayer(this.source, () -> key.then("done").translate());
            }
            this.isPreload = false;
        }
        if (this.setupTime == -1L) {
            this.setupTime = this.getExecutionTime();
        }
        this.checkTimeout();
        try {
            batchSpawnHiddenMessage.set(true);
            if (this.iterator == null) {
                this.iterator = this.players.iterator();
            }
            while (this.iterator.hasNext()) {
                if (this.isTimeExpired()) {
                    return;
                }
                NameAndId entry = iterator.next();
                ServerUtils.createFakePlayer(entry.name(), this.server, this.context);
            }
        } finally {
            batchSpawnHiddenMessage.set(false);
        }
        // 显示玩家召唤者
        if (CarpetOrgAdditionSettings.displayPlayerSummoner.get()) {
            Component summoner = KEY.then("summoner").translate(
                    this.source.getDisplayName(),
                    this.prefix + this.start,
                    this.prefix + this.end,
                    this.count
            );
            MessageUtils.broadcastMessage(this.server, summoner);
        }
        this.complete = true;
    }

    @Override
    protected long getMaxExecutionTime() {
        return 5000L + this.setupTime;
    }

    @Override
    protected long getMaxTimeSlice() {
        return 50L;
    }

    @Override
    protected boolean stopped() {
        return this.complete;
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
