package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.permission.CommandPermission;
import org.carpetorgaddition.util.permission.PermissionLevel;
import org.carpetorgaddition.util.permission.PermissionManager;

import java.io.IOException;

public class CarpetOrgAdditionCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal(CarpetOrgAddition.MOD_ID)
                .then(CommandManager.literal("permission")
                        .requires(PermissionManager.register("carpet-org-addition.permission", PermissionLevel.OWNERS))
                        .then(CommandManager.argument("node", StringArgumentType.string())
                                .suggests(suggestsNode())
                                .then(CommandManager.argument("level", StringArgumentType.string())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(PermissionLevel.listPermission(), builder))
                                        .executes(CarpetOrgAdditionCommand::setLevel))))
                .then(CommandManager.literal("version")
                        .executes(CarpetOrgAdditionCommand::version)));
    }

    private static SuggestionProvider<ServerCommandSource> suggestsNode() {
        return (context, builder) -> CommandSource.suggestMatching(
                PermissionManager.listNode().stream().map(StringArgumentType::escapeIfRequired),
                builder
        );
    }

    // 设置子命令权限
    private static int setLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        CommandPermission permission = PermissionManager.getPermission(StringArgumentType.getString(context, "node"));
        if (permission == null) {
            throw CommandUtils.createException("carpet.commands.carpet-org-addition.permission.node.not_found");
        }
        PermissionLevel level;
        try {
            level = PermissionLevel.fromString(StringArgumentType.getString(context, "level"));
        } catch (IllegalArgumentException e) {
            throw CommandUtils.createException(e, "carpet.commands.carpet-org-addition.permission.value.invalid");
        }
        permission.setLevel(level);
        MinecraftServer server = context.getSource().getServer();
        // 向服务器所有玩家发送命令树
        CommandHelper.notifyPlayersCommandsChanged(server);
        try {
            PermissionManager.save(server);
        } catch (IOException e) {
            throw CommandUtils.createIOErrorException(e);
        }
        return level.ordinal();
    }

    /**
     * 显示模组版本
     */
    private static int version(CommandContext<ServerCommandSource> context) {
        String name = CarpetOrgAddition.MOD_NAME;
        String version = CarpetOrgAddition.METADATA.getVersion().getFriendlyString();
        MessageUtils.sendMessage(context, "carpet.commands.carpet-org-addition.version", name, version);
        return 1;
    }
}
