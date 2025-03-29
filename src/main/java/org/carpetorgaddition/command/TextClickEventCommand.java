package org.carpetorgaddition.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.exception.CommandExecuteIOException;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
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

public class TextClickEventCommand {
    /**
     * 一个只有一个线程并且阻塞队列为空的线程池
     */
    private static final ThreadPoolExecutor QUERY_PLAYER_NAME_THREAD_POOL = new ThreadPoolExecutor(
            1,
            1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("textclickevent")
                .then(CommandManager.literal("queryPlayerName")
                        .then(CommandManager.argument("uuid", UuidArgumentType.uuid())
                                .executes(TextClickEventCommand::queryPlayerName))));
    }

    private static int queryPlayerName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            UuidNameMappingTable table = UuidNameMappingTable.getInstance();
            UUID uuid = UuidArgumentType.getUuid(context, "uuid");
            Optional<String> optional = table.get(uuid);
            if (optional.isPresent()) {
                // 如果本地存在，就不再从Mojang API获取
                MessageUtils.sendMessage(context, "carpet.commands.textclickevent.queryPlayerName.success", uuid.toString(), optional.get());
            } else {
                // 本地不存在，从Mojang API获取
                QUERY_PLAYER_NAME_THREAD_POOL.submit(() -> queryPlayerName(context, uuid, table));
                MessageUtils.sendMessage(context, "carpet.commands.textclickevent.queryPlayerName.start");
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
        // TODO 发送消息的代码是线程安全的吗？
        MessageUtils.sendMessage(context, "carpet.commands.textclickevent.queryPlayerName.success", uuid.toString(), name);
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
            throw CommandUtils.createException(e, "carpet.commands.textclickevent.queryPlayerName.fail", uuid.toString());
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
