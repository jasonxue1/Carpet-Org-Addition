package org.carpetorgaddition.util.provider;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.command.CommandConstants;
import org.carpetorgaddition.util.wheel.MetaComment;

import java.util.UUID;

public class CommandProvider {
    private CommandProvider() {
    }

    /**
     * 接收一件快递
     */
    public static String receiveExpress(int id) {
        return "/%s receive %s".formatted(CommandConstants.MAIL_COMMAND, id);
    }

    /**
     * 撤回一件快递
     */
    public static String cancelExpress(int id) {
        return "/%s cancel %s".formatted(CommandConstants.MAIL_COMMAND, id);
    }


    /**
     * 接收所有快递
     */
    public static String receiveAllExpress() {
        return "/%s receive".formatted(CommandConstants.MAIL_COMMAND);
    }

    /**
     * 撤回所有快递
     */
    public static String cancelAllExpress() {
        return "/%s cancel".formatted(CommandConstants.MAIL_COMMAND);
    }

    /**
     * 导航到指定UUID的实体
     */
    public static String navigateToUuidEntity(UUID uuid) {
        return "/%s uuid \"%s\"".formatted(CommandConstants.NAVIGATE_COMMAND, uuid.toString());
    }

    /**
     * 通过玩家管理器生成玩家
     */
    public static String playerManagerSpawn(String playerName) {
        return "/%s spawn %s".formatted(CommandConstants.PLAYER_MANAGER_COMMAND, playerName);
    }

    /**
     * 将一名玩家重新保存到玩家管理器
     */
    public static String playerManagerResave(String playerName, MetaComment comment) {
        String str = "/%s resave %s".formatted(CommandConstants.PLAYER_MANAGER_COMMAND, playerName);
        return comment.hasContent() ? str + " \"" + comment.getComment() + "\"" : str;
    }

    /**
     * 永久更改假玩家安全挂机阈值
     */
    public static String setupSafeAfkPermanentlyChange(EntityPlayerMPFake player, float threshold) {
        return "/%s safeafk set %s %s true".formatted(CommandConstants.PLAYER_MANAGER_COMMAND, player.getName().getString(), threshold);
    }

    /**
     * 取消设置假玩家安全挂机阈值
     */
    public static String cancelSafeAfkPermanentlyChange(EntityPlayerMPFake player) {
        return "/%s safeafk set %s -1 true".formatted(CommandConstants.PLAYER_MANAGER_COMMAND, player.getName().getString());
    }

    /**
     * 生成一名假玩家
     */
    public static String spawnFakePlayer(String playerName) {
        return "/player %s spawn".formatted(playerName);
    }

    /**
     * 让一名假玩家退出游戏
     */
    public static String killFakePlayer(String playerName) {
        return "/player %s kill".formatted(playerName);
    }

    /**
     * 高亮路径点
     */
    public static String highlightWaypoint(BlockPos blockPos) {
        return "/%s %s %s %s".formatted(CommandConstants.HIGHLIGHT_COMMAND, blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    /**
     * 使用OMMC高亮路径点
     */
    public static String highlightWaypointByOmmc(BlockPos blockPos) {
        return "/highlightWaypoint %s %s %s".formatted(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    /**
     * 通过Mojang API查询玩家名称
     */
    public static String queryPlayerName(UUID uuid) {
        return "/%s textclickevent queryPlayerName %s".formatted(CommandConstants.CARPET_ORG_ADDITION_COMMAND, uuid.toString());
    }

    /**
     * 停止导航
     */
    public static String stopNavigate() {
        return "%s stop".formatted(CommandConstants.NAVIGATE_COMMAND);
    }

    /**
     * 绘制粒子线
     */
    public static String drawParticleLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        return "particleLine " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2 + " " + z2;
    }

    /**
     * 打开玩家物品栏
     */
    public static String openPlayerInventory(PlayerEntity player) {
        return "player %s inventory".formatted(player.getName().getString());
    }
}
