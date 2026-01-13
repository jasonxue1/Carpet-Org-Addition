package boat.carpetorgaddition.util;

import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import boat.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.rule.RuleSelfManager;
import boat.carpetorgaddition.wheel.page.PageManager;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Contract;

@SuppressWarnings("unused")
public class FetcherUtils {
    private FetcherUtils() {
    }

    /**
     * 获取一名假玩家的动作管理器，永远不会返回null
     *
     * @apiNote 此方法的作用是避免IDE发出 {@code NullPointerException} 警告
     */
    @Contract("_ -> !null")
    public static FakePlayerActionManager getFakePlayerActionManager(EntityPlayerMPFake fakePlayer) {
        // TODO 不再需要
        return PlayerComponentCoordinator.getManager(fakePlayer).getFakePlayerActionManager();
    }

    @Contract("_ -> !null")
    public static BlockExcavator getBlockExcavator(EntityPlayerMPFake fakePlayer) {
        return PlayerComponentCoordinator.getManager(fakePlayer).getBlockExcavator();
    }

    public static RuleSelfManager getRuleSelfManager(MinecraftServer server) {
        return ServerComponentCoordinator.getCoordinator(server).getRuleSelfManager();
    }

    public static RuleSelfManager getRuleSelfManager(ServerPlayer player) {
        return getRuleSelfManager(ServerUtils.getServer(player));
    }

    public static PageManager getPageManager(MinecraftServer server) {
        return ServerComponentCoordinator.getCoordinator(server).getPageManager();
    }

    public static PlayerSerializationManager getFakePlayerSerializationManager(MinecraftServer server) {
        return ServerComponentCoordinator.getCoordinator(server).getPlayerSerializationManager();
    }

    public static DialogProvider getDialogProvider(MinecraftServer server) {
        return ServerComponentCoordinator.getCoordinator(server).getDialogProvider();
    }

    public static EntityPlayerActionPack getActionPack(ServerPlayer player) {
        return ((ServerPlayerInterface) player).getActionPack();
    }
}

