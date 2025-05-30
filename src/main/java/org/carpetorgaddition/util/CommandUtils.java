package org.carpetorgaddition.util;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

public class CommandUtils {
    public static final String PLAYER = "player";

    private CommandUtils() {
    }

    /**
     * 根据命令执行上下文获取命令执行者玩家对象
     *
     * @param context 用来获取玩家的命令执行上下文
     * @return 命令的执行玩家
     * @throws CommandSyntaxException 如果命令执行者不是玩家，则抛出该异常
     */
    public static ServerPlayerEntity getSourcePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return getSourcePlayer(context.getSource());
    }

    /**
     * 根据命令源获取命令执行者玩家对象
     *
     * @param source 用来获取玩家的命令源
     * @return 命令的执行玩家
     * @throws CommandSyntaxException 如果命令执行者不是玩家，则抛出该异常
     */
    public static ServerPlayerEntity getSourcePlayer(ServerCommandSource source) throws CommandSyntaxException {
        return source.getPlayerOrThrow();
    }

    /**
     * 获取命令参数中的玩家对象
     */
    public static ServerPlayerEntity getArgumentPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return EntityArgumentType.getPlayer(context, PLAYER);
    }

    /**
     * 获取命令参数中的玩家对象，并检查是不是假玩家
     */
    public static EntityPlayerMPFake getArgumentFakePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, PLAYER);
        assertFakePlayer(player);
        return (EntityPlayerMPFake) player;
    }

    /**
     * @return 指定玩家是否是命令执行者自己或假玩家
     */
    public static boolean isSelfOrFakePlayer(ServerPlayerEntity player, CommandContext<ServerCommandSource> context) {
        return isSelfOrFakePlayer(player, context.getSource());
    }

    public static boolean isSelfOrFakePlayer(ServerPlayerEntity player, ServerCommandSource source) {
        return isSpecifiedOrFakePlayer(player, source.getPlayer());
    }

    /**
     * @return 指定玩家是否是另一个指定的玩家或假玩家
     */
    public static boolean isSpecifiedOrFakePlayer(ServerPlayerEntity player, ServerPlayerEntity specified) {
        return player == specified || player instanceof EntityPlayerMPFake;
    }


    /**
     * 创建一个命令语法参数异常对象
     *
     * @param key 异常信息的翻译键
     * @return 命令语法参数异常
     */
    public static CommandSyntaxException createException(String key, Object... args) {
        return new SimpleCommandExceptionType(TextBuilder.translate(key, args)).create();
    }

    public static CommandSyntaxException createException(Throwable e, String key, Object... args) {
        String message = GameUtils.getExceptionString(e);
        TextBuilder builder = TextBuilder.of(key, args);
        builder.setHover(message);
        return new SimpleCommandExceptionType(builder.build()).create();
    }

    /**
     * @return 未找到实体
     */
    public static CommandSyntaxException createEntityNotFoundException() {
        return EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
    }

    /**
     * @return 未找到玩家
     */
    public static CommandSyntaxException createPlayerNotFoundException() {
        return EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create();
    }

    /**
     * 创建IO错误的语法参数异常异常
     */
    public static CommandSyntaxException createIOErrorException(IOException e) {
        return createException(e, "carpet.command.error.io");
    }

    /**
     * 操作超时
     */
    public static CommandSyntaxException createOperationTimeoutException() {
        return createException("carpet.command.operation.timeout");
    }

    /**
     * 只允许操作自己或假玩家
     */
    public static CommandSyntaxException createSelfOrFakePlayerException() {
        return createException("carpet.command.self_or_fake_player");
    }

    /**
     * 指定玩家不是假玩家
     */
    public static CommandSyntaxException createNotFakePlayerException(PlayerEntity fakePlayer) {
        return createException("carpet.command.not_fake_player", fakePlayer.getDisplayName());
    }

    /**
     * 断言指定玩家为假玩家。<br>
     *
     * @param fakePlayer 要检查是否为假玩家的玩家对象
     * @throws CommandSyntaxException 如果指定玩家不是假玩家
     */
    public static void assertFakePlayer(PlayerEntity fakePlayer) throws CommandSyntaxException {
        if (fakePlayer instanceof EntityPlayerMPFake) {
            return;
        }
        // 不是假玩家时抛出异常
        throw createNotFakePlayerException(fakePlayer);
    }

    /**
     * 从字符串解析一个UUID
     */
    public static UUID parseUuidFromString(String uuid) throws CommandSyntaxException {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw createException("carpet.command.uuid.parse.fail");
        }
    }

    /**
     * 让一名玩家执行一条命令，命令的前缀“/”可有可无，但不建议有
     */
    public static void execute(ServerPlayerEntity player, String command) {
        CommandUtils.execute(player.getCommandSource(), command);
    }

    public static void execute(ServerCommandSource source, String command) {
        CommandManager commandManager = source.getServer().getCommandManager();
        commandManager.executeWithPrefix(source, command);
    }

    public static void handlingException(ThrowingRunnable runnable, CommandContext<ServerCommandSource> context) {
        handlingException(runnable, context.getSource());
    }

    public static void handlingException(ThrowingRunnable runnable, ServerCommandSource source) {
        try {
            runnable.run();
        } catch (CommandSyntaxException e) {
            MessageUtils.sendVanillaErrorMessage(source, e);
        }
    }

    /**
     * @return 玩家是否有执行某一命令的权限
     * @see CommandHelper#canUseCommand(ServerCommandSource, Object)
     */
    public static boolean canUseCommand(int level, Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case String str -> switch (str.toLowerCase(Locale.ROOT)) {
                case "ops", "2" -> level >= 2;
                case "0", "1", "3", "4" -> level >= Integer.parseInt(str);
                case "true" -> true;
                default -> false;
            };
            case null -> false;
            default -> canUseCommand(level, value.toString());
        };
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws CommandSyntaxException;
    }
}
