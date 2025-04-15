package org.carpetorgaddition.util.provider;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.wheel.Annotation;

import java.util.UUID;

public class CommandProvider {
    private CommandProvider() {
    }

    /**
     * 接收一件快递
     */
    public static String receiveExpress(int id) {
        return "/mail receive %s".formatted(id);
    }

    /**
     * 撤回一件快递
     */
    public static String cancelExpress(int id) {
        return "/mail cancel %s".formatted(id);
    }


    /**
     * 接收所有快递
     */
    public static String receiveAllExpress() {
        return "/mail receive";
    }

    /**
     * 撤回所有快递
     */
    public static String cancelAllExpress() {
        return "/mail cancel";
    }

    /**
     * 导航到指定UUID的实体
     */
    public static String navigateToUuidEntity(UUID uuid) {
        return "/navigate uuid \"%s\"".formatted(uuid.toString());
    }

    /**
     * 通过玩家管理器生成玩家
     */
    public static String playerManagerSpawn(String playerName) {
        return "/playerManager spawn %s".formatted(playerName);
    }

    /**
     * 将一名玩家重新保存到玩家管理器
     */
    public static String playerManagerResave(String playerName, Annotation annotation) {
        String str = "/playerManager resave %s".formatted(playerName);
        return annotation.hasContent() ? str + " \"" + annotation.getAnnotation() + "\"" : str;
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
        return "/highlight %s %s %s".formatted(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    /**
     * 使用OMMC高亮路径点
     */
    public static String highlightWaypointByOmmc(BlockPos blockPos) {
        return "/highlightWaypoint %s %s %s".formatted(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    /**
     * 永久更改假玩家安全挂机阈值
     */
    public static String setupSafeAfkPermanentlyChange(EntityPlayerMPFake player, float threshold) {
        return "/playerManager safeafk set %s %s true".formatted(player.getName().getString(), threshold);
    }

    /**
     * 取消设置假玩家安全挂机阈值
     */
    public static String cancelSafeAfkPermanentlyChange(EntityPlayerMPFake player) {
        return "/playerManager safeafk set %s -1 true".formatted(player.getName().getString());
    }

    /**
     * 通过Mojang API查询玩家名称
     */
    public static String queryPlayerName(UUID uuid) {
        return "/%s textclickevent queryPlayerName %s".formatted(CarpetOrgAddition.MOD_ID, uuid.toString());
    }

    /**
     * 停止导航
     */
    public static String stopNavigate() {
        return "navigate stop";
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
