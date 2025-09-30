package org.carpetorgaddition.util;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.carpetorgaddition.wheel.CreateFakePlayerContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class GenericUtils {
    /**
     * 当假玩家正在生成时，执行此函数
     */
    public static final ThreadLocal<Consumer<EntityPlayerMPFake>> FAKE_PLAYER_SPAWNING = new ThreadLocal<>();
    /**
     * {@link EntityPlayerMPFake#createFake(String, MinecraftServer, Vec3d, double, double, RegistryKey, GameMode, boolean)}内部的lambda表达式执行时，调用次函数
     */
    public static final ThreadLocal<Consumer<EntityPlayerMPFake>> INTERNAL_FAKE_PLAYER_SPAWNING = new ThreadLocal<>();
    /**
     * 当前{@code Minecraft}的NBT数据版本
     */
    public static final int CURRENT_DATA_VERSION = getNbtDataVersion();

    private GenericUtils() {
    }

    /**
     * 根据UUID获取实体
     */
    @Nullable
    public static Entity getEntity(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 根据UUID获取玩家
     */
    public static Optional<ServerPlayerEntity> getPlayer(MinecraftServer server, UUID uuid) {
        return Optional.ofNullable(server.getPlayerManager().getPlayer(uuid));
    }

    /**
     * 根据名称获取玩家
     */
    public static Optional<ServerPlayerEntity> getPlayer(MinecraftServer server, String name) {
        return Optional.ofNullable(server.getPlayerManager().getPlayer(name));
    }

    public static Optional<ServerPlayerEntity> getPlayer(MinecraftServer server, GameProfile gameProfile) {
        return getPlayer(server, gameProfile.getName());
    }

    /**
     * 创建一个假玩家
     */
    public static void createFakePlayer(String username, MinecraftServer server, CreateFakePlayerContext context) {
        createFakePlayer(username, server, context.pos(), context.yaw(), context.pitch(), context.dimension(), context.gamemode(), context.flying(), context.consumer());
    }

    public static Optional<UUID> uuidFromString(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(str));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * 创建一个假玩家
     *
     * @param consumer 玩家生成时执行的函数
     */
    public static void createFakePlayer(String username, MinecraftServer server, Vec3d pos, double yaw, double pitch, RegistryKey<World> dimension, GameMode gamemode, boolean flying, Consumer<EntityPlayerMPFake> consumer) {
        try {
            FAKE_PLAYER_SPAWNING.set(consumer);
            EntityPlayerMPFake.createFake(username, server, pos, yaw, pitch, dimension, gamemode, flying);
        } finally {
            FAKE_PLAYER_SPAWNING.remove();
        }
    }

    /**
     * @return 获取异常的类名+消息形式，如果没有消息，返回异常类的简单类名
     * @apiNote 不使用 {@code toString} 方法是因为方法可能被子类重写
     */
    public static String getExceptionString(Throwable throwable) {
        String name = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        return message == null ? name : name + ": " + message;
    }

    /**
     * @return 当前游戏的NBT数据版本
     */
    public static int getNbtDataVersion() {
        return SharedConstants.getGameVersion().dataVersion().id();
    }

    /**
     * 一个占位符，什么也不做
     */
    public static void pass(Object... ignored) {
    }
}
