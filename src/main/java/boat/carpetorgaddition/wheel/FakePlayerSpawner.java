package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.CarpetOrgAddition;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class FakePlayerSpawner {
    /**
     * 假玩家在上线或下线时，是否隐藏上下线的消息
     */
    public static final ScopedValue<Boolean> HIDDEN_MESSAGE = ScopedValue.newInstance();
    /**
     * 假玩家生成后执行的回调函数
     */
    public static final ScopedValue<Consumer<EntityPlayerMPFake>> CALLBACK = ScopedValue.newInstance();
    /**
     * 玩家的名称
     */
    private final String name;
    private final MinecraftServer server;
    /**
     * 假玩家生成的位置
     */
    private Vec3 position;
    /**
     * 假玩家生成的维度
     */
    private ResourceKey<Level> dimension;
    /**
     * 水平方向的朝向
     */
    private double yaw = 0.0;
    /**
     * 垂直方向的朝向
     */
    private double pitch = 0.0;
    /**
     * 游戏模式
     */
    private GameType gameMode;
    /**
     * 是否正在鞘翅飞行
     */
    private boolean flying = false;
    /**
     * 在假玩家生成后执行
     */
    private Consumer<EntityPlayerMPFake> callback = CarpetOrgAddition::pass;
    /**
     * 是否隐藏登录消息
     */
    private boolean silence;

    private FakePlayerSpawner(MinecraftServer server, String name) {
        this.server = server;
        this.name = name;
        this.position = server.getRespawnData().pos().getBottomCenter();
        this.dimension = server.overworld().dimension();
        this.gameMode = server.getDefaultGameType();
    }

    public static FakePlayerSpawner of(MinecraftServer server, String name) {
        return new FakePlayerSpawner(server, name);
    }

    public FakePlayerSpawner setPosition(Vec3 position) {
        this.position = position;
        return this;
    }

    public FakePlayerSpawner setGameMode(GameType gameMode) {
        this.gameMode = gameMode;
        return this;
    }

    public FakePlayerSpawner setWorld(Level world) {
        return this.setWorld(world.dimension());
    }

    public FakePlayerSpawner setWorld(ResourceKey<Level> world) {
        this.dimension = world;
        return this;
    }

    public FakePlayerSpawner setYaw(double yaw) {
        this.yaw = yaw;
        return this;
    }

    public FakePlayerSpawner setPitch(double pitch) {
        this.pitch = pitch;
        return this;
    }

    public FakePlayerSpawner setFlying(boolean flying) {
        this.flying = flying;
        return this;
    }

    public FakePlayerSpawner setCallback(Consumer<EntityPlayerMPFake> callback) {
        this.callback = callback;
        return this;
    }

    public FakePlayerSpawner setSilence(boolean silence) {
        this.silence = silence;
        return this;
    }

    public void spawn() {
        ScopedValue.where(HIDDEN_MESSAGE, this.silence)
                .where(CALLBACK, this.callback)
                .run(() -> EntityPlayerMPFake.createFake(this.name, this.server, this.position, this.yaw, this.pitch, this.dimension, this.gameMode, this.flying));
    }
}
