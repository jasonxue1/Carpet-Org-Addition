package boat.carpetorgaddition.util;

import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.*;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
    public static ServerPlayer getSourcePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return getSourcePlayer(context.getSource());
    }

    /**
     * 根据命令源获取命令执行者玩家对象
     *
     * @param source 用来获取玩家的命令源
     * @return 命令的执行玩家
     * @throws CommandSyntaxException 如果命令执行者不是玩家，则抛出该异常
     */
    public static ServerPlayer getSourcePlayer(CommandSourceStack source) throws CommandSyntaxException {
        return source.getPlayerOrException();
    }

    public static Optional<ServerPlayer> getSourcePlayerNullable(CommandContext<CommandSourceStack> context) {
        return getSourcePlayerNullable(context.getSource());
    }

    public static Optional<ServerPlayer> getSourcePlayerNullable(CommandSourceStack source) {
        return Optional.ofNullable(source.getPlayer());
    }

    /**
     * 获取命令参数中的玩家对象
     */
    public static ServerPlayer getArgumentPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return EntityArgument.getPlayer(context, PLAYER);
    }

    /**
     * 获取命令参数中的玩家对象，并检查是不是假玩家
     */
    public static EntityPlayerMPFake getArgumentFakePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER);
        assertFakePlayer(player);
        return (EntityPlayerMPFake) player;
    }

    public static Collection<NameAndId> getGameProfiles(CommandContext<CommandSourceStack> context, String arguments) throws CommandSyntaxException {
        return GameProfileArgument.getGameProfiles(context, arguments);
    }

    public static GameProfile getGameProfile(CommandContext<CommandSourceStack> context, String arguments) throws CommandSyntaxException {
        Collection<GameProfile> collection = getGameProfiles(context, arguments).stream()
                .map(entry -> new GameProfile(entry.id(), entry.name()))
                .toList();
        return switch (collection.size()) {
            case 0 -> throw GameProfileArgument.ERROR_UNKNOWN_PLAYER.create();
            case 1 -> collection.iterator().next();
            default -> {
                TextBuilder builder = TextBuilder.of("carpet.command.argument.player.toomany");
                ArrayList<Component> list = new ArrayList<>();
                for (GameProfile gameProfile : collection) {
                    list.add(TextBuilder.translate("%s: %s", EntityType.PLAYER.getDescription(), gameProfile.name()));
                }
                builder.setHover(TextBuilder.joinList(list));
                throw createException(builder.build());
            }
        };
    }

    /**
     * @return 指定玩家是否是命令执行者自己或假玩家
     */
    public static boolean isSelfOrFakePlayer(ServerPlayer player, CommandContext<CommandSourceStack> context) {
        return isSelfOrFakePlayer(player, context.getSource());
    }

    public static boolean isSelfOrFakePlayer(ServerPlayer player, CommandSourceStack source) {
        return isSpecifiedOrFakePlayer(player, source.getPlayer());
    }

    /**
     * @return 指定玩家是否是另一个指定的玩家或假玩家
     */
    public static boolean isSpecifiedOrFakePlayer(ServerPlayer player, ServerPlayer specified) {
        return player == specified || player instanceof EntityPlayerMPFake;
    }


    /**
     * 创建一个命令语法参数异常对象
     *
     * @param key 异常信息的翻译键
     * @return 命令语法参数异常
     */
    public static CommandSyntaxException createException(String key, Object... args) {
        return createException(TextBuilder.translate(key, args));
    }

    public static CommandSyntaxException createException(Throwable e, String key, Object... args) {
        String message = GenericUtils.getExceptionString(e);
        TextBuilder builder = TextBuilder.of(key, args);
        builder.setHover(message);
        return new SimpleCommandExceptionType(builder.build()).create();
    }

    public static CommandSyntaxException createException(Component text) {
        return new SimpleCommandExceptionType(text).create();
    }

    /**
     * @return 未找到实体
     */
    public static CommandSyntaxException createEntityNotFoundException() {
        return EntityArgument.NO_ENTITIES_FOUND.create();
    }

    /**
     * @return 未找到玩家
     */
    public static CommandSyntaxException createPlayerNotFoundException() {
        return EntityArgument.NO_PLAYERS_FOUND.create();
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
    public static CommandSyntaxException createNotFakePlayerException(Player fakePlayer) {
        return createException("carpet.command.not_fake_player", fakePlayer.getDisplayName());
    }

    /**
     * 断言指定玩家为假玩家。<br>
     *
     * @param fakePlayer 要检查是否为假玩家的玩家对象
     * @throws CommandSyntaxException 如果指定玩家不是假玩家
     */
    public static void assertFakePlayer(Player fakePlayer) throws CommandSyntaxException {
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
    public static void execute(ServerPlayer player, String command) {
        CommandUtils.execute(player.createCommandSourceStack(), command);
    }

    public static void execute(CommandSourceStack source, String command) {
        Commands commandManager = source.getServer().getCommands();
        commandManager.performPrefixedCommand(source, command);
    }

    @SuppressWarnings("unused")
    public static void handlingException(ThrowingRunnable runnable, CommandContext<CommandSourceStack> context) {
        handlingException(runnable, context.getSource());
    }

    public static void handlingException(ThrowingRunnable runnable, CommandSourceStack source) {
        try {
            runnable.run();
        } catch (CommandSyntaxException e) {
            handlingException(e, source);
        }
    }

    public static void handlingException(CommandSyntaxException e, CommandSourceStack source) {
        MessageUtils.sendVanillaErrorMessage(source, e);
    }

    /**
     * @return 玩家是否有执行某一命令的权限
     * @see CommandHelper#canUseCommand(CommandSourceStack, Object)
     */
    public static boolean canUseCommand(int level, Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case String str -> switch (str.toLowerCase(Locale.ROOT)) {
                case "ops", "2" -> level >= 2;
                case "1", "3", "4" -> level >= Integer.parseInt(str);
                case "0", "true" -> true;
                default -> false;
            };
            case null -> false;
            default -> canUseCommand(level, value.toString());
        };
    }

    /**
     * @return 玩家是否有执行某一命令的权限
     */
    public static boolean canUseCommand(PermissionCheck permissionCheck, Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case String str -> switch (str.toLowerCase(Locale.ROOT)) {
                case "ops", "2" -> permissionCheck.check(parsePermissionPredicate(2));
                case "1", "3", "4" -> permissionCheck.check(parsePermissionPredicate(Integer.parseInt(str)));
                case "0", "true" -> true;
                default -> false;
            };
            case null, default -> false;
        };
    }

    public static boolean canUseCommand(PermissionSet predicate, Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case String str -> switch (str.toLowerCase(Locale.ROOT)) {
                case "ops", "2" -> predicate.hasPermission(parsePermission(2));
                case "1", "3", "4" -> predicate.hasPermission(parsePermission(Integer.parseInt(str)));
                case "0", "true" -> true;
                default -> false;
            };
            case null, default -> false;
        };
    }

    private static LevelBasedPermissionSet parsePermissionPredicate(int level) {
        return LevelBasedPermissionSet.forLevel(PermissionLevel.byId(level));
    }

    public static Permission parsePermission(int level) {
        return new Permission.HasCommandLevel(PermissionLevel.byId(level));
    }

    /**
     * @return 玩家是否有执行某一命令的权限
     * @see CommandHelper#canUseCommand(CommandSourceStack, Object)
     */
    public static Predicate<CommandSourceStack> canUseCommand(Supplier<String> supplier) {
        return source -> canUseCommand(source, supplier.get());
    }

    public static boolean canUseCommand(CommandSourceStack source, Supplier<String> supplier) {
        return canUseCommand(source, supplier.get());
    }

    public static boolean canUseCommand(CommandSourceStack source, String rule) {
        return CommandHelper.canUseCommand(source, rule);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws CommandSyntaxException;
    }
}
