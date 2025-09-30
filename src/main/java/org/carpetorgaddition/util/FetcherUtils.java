package org.carpetorgaddition.util;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.periodic.PlayerComponentCoordinator;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import org.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import org.carpetorgaddition.rule.RuleSelfManager;
import org.carpetorgaddition.wheel.page.PageManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class FetcherUtils {
    private FetcherUtils() {
    }

    /**
     * 获取一名玩家的字符串形式的玩家名
     *
     * @param player 要获取字符串形式玩家名的玩家
     * @return 玩家名的字符串形式
     */
    public static String getPlayerName(PlayerEntity player) {
        return player.getGameProfile().name();
    }

    @Contract("_ -> !null")
    public static MinecraftServer getServer(ServerPlayerEntity player) {
        return getWorld(player).getServer();
    }

    @Nullable
    public static MinecraftServer getServer(Entity entity) {
        return getWorld(entity).getServer();
    }

    public static ServerWorld getWorld(ServerPlayerEntity player) {
        return player.getEntityWorld();
    }

    public static World getWorld(Entity entity) {
        return entity.getEntityWorld();
    }

    public static World getWorld(BlockEntity blockEntity) {
        return blockEntity.getWorld();
    }

    public static Vec3d getFootPos(Entity entity) {
        return entity.getEntityPos();
    }

    public static Vec3d getEyePos(Entity entity) {
        return entity.getEyePos();
    }

    /**
     * 获取一名假玩家的动作管理器，永远不会返回null
     *
     * @apiNote 此方法的作用是避免IDE发出 {@code NullPointerException} 警告
     */
    @Contract("_ -> !null")
    public static FakePlayerActionManager getFakePlayerActionManager(EntityPlayerMPFake fakePlayer) {
        return PlayerComponentCoordinator.getManager(fakePlayer).getFakePlayerActionManager();
    }

    @Contract("_ -> !null")
    public static BlockExcavator getBlockExcavator(EntityPlayerMPFake fakePlayer) {
        return PlayerComponentCoordinator.getManager(fakePlayer).getBlockExcavator();
    }

    public static RuleSelfManager getRuleSelfManager(MinecraftServer server) {
        return ServerComponentCoordinator.getCoordinator(server).getRuleSelfManager();
    }

    public static RuleSelfManager getRuleSelfManager(ServerPlayerEntity player) {
        return getRuleSelfManager(FetcherUtils.getServer(player));
    }

    public static PageManager getPageManager(MinecraftServer server) {
        return ServerComponentCoordinator.getCoordinator(server).getPageManager();
    }

    public static PlayerSerializationManager getFakePlayerSerializationManager(MinecraftServer server) {
        return ServerComponentCoordinator.getCoordinator(server).getPlayerSerializationManager();
    }
}

