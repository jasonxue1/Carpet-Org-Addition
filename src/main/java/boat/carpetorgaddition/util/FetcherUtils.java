package boat.carpetorgaddition.util;

import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import boat.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.rule.RuleSelfManager;
import boat.carpetorgaddition.wheel.page.PageManager;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class FetcherUtils {
    private FetcherUtils() {
    }

    /**
     * 获取一名玩家的字符串形式的玩家名
     *
     * @param player 要获取字符串形式玩家名的玩家
     * @return 玩家名的字符串形式
     */
    public static String getPlayerName(Player player) {
        return player.getGameProfile().name();
    }

    @Contract("_ -> !null")
    public static MinecraftServer getServer(ServerPlayer player) {
        return getWorld(player).getServer();
    }

    @Nullable
    public static MinecraftServer getServer(Entity entity) {
        return getWorld(entity).getServer();
    }

    public static ServerLevel getWorld(ServerPlayer player) {
        return player.level();
    }

    public static Level getWorld(Entity entity) {
        return entity.level();
    }

    public static ServerLevel getWorld(CommandSourceStack source) {
        return source.getLevel();
    }

    public static Level getWorld(BlockEntity blockEntity) {
        return blockEntity.getLevel();
    }

    public static Vec3 getFootPos(Entity entity) {
        return entity.position();
    }

    public static Vec3 getEyePos(Entity entity) {
        return entity.getEyePosition();
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

    public static RuleSelfManager getRuleSelfManager(ServerPlayer player) {
        return getRuleSelfManager(FetcherUtils.getServer(player));
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
}

