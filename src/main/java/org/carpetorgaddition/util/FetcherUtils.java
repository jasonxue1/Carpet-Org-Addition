package org.carpetorgaddition.util;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.periodic.PlayerComponentCoordinator;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import org.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import org.carpetorgaddition.rule.RuleSelfManager;
import org.carpetorgaddition.wheel.page.PageManager;
import org.jetbrains.annotations.Contract;

public class FetcherUtils {
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
        return ServerComponentCoordinator.getManager(server).getRuleSelfManager();
    }

    public static RuleSelfManager getRuleSelfManager(ServerPlayerEntity player) {
        return getRuleSelfManager(player.getServer());
    }

    public static PageManager getPageManager(MinecraftServer server) {
        return ServerComponentCoordinator.getManager(server).getPageManager();
    }

    public static PlayerSerializationManager getFakePlayerSerializationManager(MinecraftServer server) {
        return ServerComponentCoordinator.getManager(server).getPlayerSerializationManager();
    }
}

