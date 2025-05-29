package org.carpetorgaddition.util.provider;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.client.command.AbstractClientCommand;
import org.carpetorgaddition.client.command.ClientCommandRegister;
import org.carpetorgaddition.client.command.HighlightCommand;
import org.carpetorgaddition.command.*;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;
import java.util.UUID;

public class CommandProvider {
    private CommandProvider() {
    }

    /**
     * 接收一件快递
     */
    public static String receiveExpress(int id) {
        return "/%s receive %s".formatted(getCommandName(MailCommand.class), id);
    }

    /**
     * 撤回一件快递
     */
    public static String cancelExpress(int id) {
        return "/%s cancel %s".formatted(getCommandName(MailCommand.class), id);
    }

    /**
     * 接收所有快递
     */
    public static String receiveAllExpress() {
        return "/%s receive".formatted(getCommandName(MailCommand.class));
    }

    /**
     * 撤回所有快递
     */
    public static String cancelAllExpress() {
        return "/%s cancel".formatted(getCommandName(MailCommand.class));
    }

    /**
     * 导航到指定UUID的实体
     */
    public static String navigateToUuidEntity(UUID uuid) {
        return "/%s uuid \"%s\"".formatted(getCommandName(NavigatorCommand.class), uuid.toString());
    }

    /**
     * 通过玩家管理器生成玩家
     */
    public static String playerManagerSpawn(String playerName) {
        return "/%s spawn %s".formatted(getCommandName(PlayerManagerCommand.class), playerName);
    }

    /**
     * 将一名玩家重新保存到玩家管理器
     */
    public static String playerManagerResave(String playerName) {
        return "/%s modify resave %s".formatted(getCommandName(PlayerManagerCommand.class), playerName);
    }

    /**
     * 永久更改假玩家安全挂机阈值
     */
    public static String setupSafeAfkPermanentlyChange(EntityPlayerMPFake player, float threshold) {
        String commandName = getCommandName(PlayerManagerCommand.class);
        String playerName = player.getName().getString();
        return "/%s safeafk set %s %s true".formatted(commandName, playerName, threshold);
    }

    /**
     * 取消设置假玩家安全挂机阈值
     */
    public static String cancelSafeAfkPermanentlyChange(EntityPlayerMPFake player) {
        String commandName = getCommandName(PlayerManagerCommand.class);
        String playerName = player.getName().getString();
        return "/%s safeafk set %s -1 true".formatted(commandName, playerName);
    }

    public static String listGroupPlayer(String group, @Nullable String filter) {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(PlayerManagerCommand.class));
        joiner.add("group list group");
        joiner.add(StringArgumentType.escapeIfRequired(group));
        if (filter != null) {
            joiner.add(StringArgumentType.escapeIfRequired(filter));
        }
        return joiner.toString();
    }

    public static String listUngroupedPlayer(@Nullable String filter) {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(PlayerManagerCommand.class));
        joiner.add("group list ungrouped");
        if (filter != null) {
            joiner.add(StringArgumentType.escapeIfRequired(filter));
        }
        return joiner.toString();
    }

    public static String listAllPlayer(@Nullable String filter) {
        StringJoiner joiner = new StringJoiner(" ", "/", "");
        joiner.add(getCommandName(PlayerManagerCommand.class));
        joiner.add("group list all");
        if (filter != null) {
            joiner.add(StringArgumentType.escapeIfRequired(filter));
        }
        return joiner.toString();
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
        String name = getClientCommandName(HighlightCommand.class, HighlightCommand.DEFAULT_COMMAND_NAME);
        return "/%s %s %s %s".formatted(name, blockPos.getX(), blockPos.getY(), blockPos.getZ());
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
        return "/%s textclickevent queryPlayerName %s".formatted(getCommandName(OrangeCommand.class), uuid.toString());
    }

    /**
     * 停止导航
     */
    public static String stopNavigate() {
        return "%s stop".formatted(getCommandName(NavigatorCommand.class));
    }

    /**
     * 绘制粒子线
     */
    public static String drawParticleLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        return "particleLine %s %s %s %s %s %s".formatted(x1, y1, z1, x2, y2, z2);
    }

    /**
     * 打开玩家物品栏
     */
    public static String openPlayerInventory(PlayerEntity player) {
        return "player %s inventory".formatted(player.getName().getString());
    }

    public static String openPlayerInventory(UUID uuid) {
        return "/%s textclickevent openInventory %s inventory".formatted(getCommandName(OrangeCommand.class), uuid.toString());
    }

    public static String openPlayerEnderChest(UUID uuid) {
        return "/%s textclickevent openInventory %s enderChest".formatted(getCommandName(OrangeCommand.class), uuid.toString());
    }

    /**
     * 翻页
     */
    public static String pageTurning(int id, int number) {
        return "/%s textclickevent pageturning %s %s".formatted(getCommandName(OrangeCommand.class), id, number);
    }

    public static String setCarpetRule(String rule, String value) {
        return "/carpet %s %s".formatted(rule, value);
    }

    private static <T extends AbstractServerCommand> String getCommandName(Class<T> clazz) {
        return CommandRegister.getCommandInstance(clazz).getAvailableName();
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends AbstractClientCommand> String getClientCommandName(Class<T> clazz, String other) {
        T instance = ClientCommandRegister.getCommandInstance(clazz);
        if (instance == null) {
            return other;
        }
        return instance.getAvailableName();
    }
}
