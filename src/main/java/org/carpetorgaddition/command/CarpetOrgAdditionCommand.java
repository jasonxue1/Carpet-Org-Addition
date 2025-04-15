package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.exception.CommandExecuteIOException;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.permission.CommandPermission;
import org.carpetorgaddition.util.permission.PermissionLevel;
import org.carpetorgaddition.util.permission.PermissionManager;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.UuidNameMappingTable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CarpetOrgAdditionCommand {
    /**
     * 一个只有一个线程并且阻塞队列为空的线程池
     */
    private static final ThreadPoolExecutor QUERY_PLAYER_NAME_THREAD_POOL = new ThreadPoolExecutor(
            0,
            1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.AbortPolicy()
    );

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
                        .executes(CarpetOrgAdditionCommand::version))
                .then(CommandManager.literal("textclickevent")
                        .then(CommandManager.literal("queryPlayerName")
                                .then(CommandManager.argument("uuid", UuidArgumentType.uuid())
                                        .executes(CarpetOrgAdditionCommand::queryPlayerName)))));
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

    private static int queryPlayerName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            UuidNameMappingTable table = UuidNameMappingTable.getInstance();
            UUID uuid = UuidArgumentType.getUuid(context, "uuid");
            Optional<String> optional = table.get(uuid);
            if (optional.isPresent()) {
                // 如果本地存在，就不再从Mojang API获取
                String playerUuid = uuid.toString();
                String playerName = optional.get();
                sendFeekback(context, playerUuid, playerName);
            } else {
                // 本地不存在，从Mojang API获取
                QUERY_PLAYER_NAME_THREAD_POOL.submit(() -> queryPlayerName(context, uuid, table));
                MessageUtils.sendMessage(context, "carpet.commands.carpet-org-addition.textclickevent.queryPlayerName.start");
            }
        } catch (RejectedExecutionException e) {
            // 只允许同时存在一个线程执行查询任务
            throw CommandUtils.createException("carpet.command.thread.wait.last");
        }
        return 1;
    }

    /**
     * 在独立线程查询玩家名称
     */
    private static void queryPlayerName(CommandContext<ServerCommandSource> context, UUID uuid, UuidNameMappingTable table) {
        String name;
        try {
            name = queryPlayerNameFromMojangApi(uuid);
        } catch (CommandSyntaxException e) {
            context.getSource().handleException(e, false, null);
            return;
        }
        table.put(uuid, name);
        MinecraftServer server = context.getSource().getServer();
        // 在服务器线程发送命令反馈
        server.execute(() -> sendFeekback(context, uuid.toString(), name));
    }

    private static void sendFeekback(CommandContext<ServerCommandSource> context, String playerUuid, String playerName) {
        MessageUtils.sendMessage(
                context,
                "carpet.commands.carpet-org-addition.textclickevent.queryPlayerName.success",
                TextUtils.copy(playerUuid, playerUuid, TextProvider.COPY_CLICK, Formatting.GRAY),
                TextUtils.copy(playerName, playerName, TextProvider.COPY_CLICK, Formatting.GRAY)
        );
    }

    /**
     * 通过Mojang API查询玩家名称
     */
    private static String queryPlayerNameFromMojangApi(UUID uuid) throws CommandSyntaxException {
        URL url;
        try {
            URI uri = new URI(UuidNameMappingTable.MOJANG_API.formatted(uuid.toString()));
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw CommandUtils.createException(e, "carpet.command.url.parse.fail");
        }
        URLConnection connection;
        try {
            // 连接到Mojang API
            connection = url.openConnection();
        } catch (IOException e) {
            throw CommandUtils.createException(e, "carpet.command.api.mojang_api.connection.fail", uuid.toString());
        }
        InputStream input;
        BufferedReader reader;
        try {
            // 获取字节流并转换为字符流
            input = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));
        } catch (IOException e) {
            throw CommandUtils.createException(e, "carpet.commands.carpet-org-addition.textclickevent.queryPlayerName.fail", uuid.toString());
        }
        StringBuilder sb = new StringBuilder();
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
        // 解析json字符串
        JsonObject json = IOUtils.GSON.fromJson(sb.toString(), JsonObject.class);
        if (json.has("name")) {
            return json.get("name").getAsString();
        }
        throw CommandUtils.createException("carpet.command.api.mojang_api.connection.fail", uuid.toString());
    }
}
