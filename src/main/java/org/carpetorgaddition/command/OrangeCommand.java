package org.carpetorgaddition.command;

import carpet.api.settings.CarpetRule;
import carpet.utils.CommandHelper;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.CommandExecuteIOException;
import org.carpetorgaddition.rule.RuleSelfManager;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.rule.value.OpenPlayerInventory;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.GenericFetcherUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.inventory.OfflinePlayerInventory;
import org.carpetorgaddition.util.page.PageManager;
import org.carpetorgaddition.util.page.PagedCollection;
import org.carpetorgaddition.util.permission.CommandPermission;
import org.carpetorgaddition.util.permission.PermissionLevel;
import org.carpetorgaddition.util.permission.PermissionManager;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.TextBuilder;
import org.carpetorgaddition.util.wheel.UuidNameMappingTable;
import org.jetbrains.annotations.NotNull;

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

public class OrangeCommand extends AbstractServerCommand {
    /**
     * 一个只有一个线程并且阻塞队列为空的线程池
     */
    private final ThreadPoolExecutor QUERY_PLAYER_NAME_THREAD_POOL = new ThreadPoolExecutor(
            0,
            1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            this::createDaemonThread,
            new ThreadPoolExecutor.AbortPolicy()
    );

    public OrangeCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    /**
     * 创建守护线程
     */
    private Thread createDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        // 设置守护线程
        thread.setDaemon(true);
        thread.setName("Query-Player-Name");
        return thread;
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .then(CommandManager.literal("permission")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("node", StringArgumentType.string())
                                .suggests(suggestsNode())
                                .then(CommandManager.argument("level", StringArgumentType.string())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(PermissionLevel.listPermission(), builder))
                                        .executes(this::setLevel))))
                .then(CommandManager.literal("version")
                        .executes(this::version))
                .then(CommandManager.literal("ruleself")
                        .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                .then(CommandManager.argument("rule", StringArgumentType.string())
                                        .suggests(suggestRule())
                                        .executes(this::infoRuleSelf)
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(this::setRuleSelf)))))
                .then(CommandManager.literal("textclickevent")
                        .then(CommandManager.literal("queryPlayerName")
                                .then(CommandManager.argument("uuid", UuidArgumentType.uuid())
                                        .executes(this::queryPlayerName)))
                        .then(CommandManager.literal("pageturning")
                                .then(CommandManager.argument("id", IntegerArgumentType.integer(0))
                                        .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                                .executes(this::pageTurning))))
                        .then(CommandManager.literal("openInventory")
                                .requires(OpenPlayerInventory::isEnable)
                                .then(CommandManager.argument("uuid", UuidArgumentType.uuid())
                                        .then(CommandManager.literal("inventory")
                                                .executes(context -> openPlayerInventory(context, true)))
                                        .then(CommandManager.literal("enderChest")
                                                .executes(context -> openPlayerInventory(context, false)))))));
    }

    private @NotNull SuggestionProvider<ServerCommandSource> suggestRule() {
        return (context, builder) -> CommandSource.suggestMatching(RuleSelfManager.RULES.values().stream().map(CarpetRule::name), builder);
    }

    private SuggestionProvider<ServerCommandSource> suggestsNode() {
        return (context, builder) -> CommandSource.suggestMatching(
                PermissionManager.listNode().stream().map(StringArgumentType::escapeIfRequired),
                builder
        );
    }

    // 设置子命令权限
    private int setLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        CommandPermission permission = PermissionManager.getPermission(StringArgumentType.getString(context, "node"));
        if (permission == null) {
            throw CommandUtils.createException("carpet.commands.orange.permission.node.not_found");
        }
        PermissionLevel level;
        try {
            level = PermissionLevel.fromString(StringArgumentType.getString(context, "level"));
        } catch (IllegalArgumentException e) {
            throw CommandUtils.createException(e, "carpet.commands.orange.permission.value.invalid");
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
    private int version(CommandContext<ServerCommandSource> context) {
        String name = CarpetOrgAddition.MOD_NAME;
        MutableText version = new TextBuilder(CarpetOrgAddition.VERSION).setHover(CarpetOrgAddition.BUILD_TIMESTAMP).build();
        MessageUtils.sendMessage(context, "carpet.commands.orange.version", name, version);
        return 1;
    }

    private int queryPlayerName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
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
                MessageUtils.sendMessage(context, "carpet.commands.orange.textclickevent.queryPlayerName.start");
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
    private void queryPlayerName(CommandContext<ServerCommandSource> context, UUID uuid, UuidNameMappingTable table) {
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

    private void sendFeekback(CommandContext<ServerCommandSource> context, String playerUuid, String playerName) {
        MutableText uuid = new TextBuilder(playerUuid).setCopyToClipboard(playerUuid).setColor(Formatting.GRAY).build();
        MutableText name = new TextBuilder(playerName).setCopyToClipboard(playerName).setColor(Formatting.GRAY).build();
        MessageUtils.sendMessage(context, "carpet.commands.orange.textclickevent.queryPlayerName.success", uuid, name);
    }

    /**
     * 通过Mojang API查询玩家名称
     */
    private String queryPlayerNameFromMojangApi(UUID uuid) throws CommandSyntaxException {
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
            throw CommandUtils.createException(e, "carpet.commands.orange.textclickevent.queryPlayerName.fail", uuid.toString());
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

    // 设置一条规则是否对自己生效
    private int setRuleSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getArgumentPlayer(context);
        if (CommandUtils.isSelfOrFakePlayer(player, context)) {
            RuleSelfManager ruleSelfManager = GenericFetcherUtils.getRuleSelfManager(player);
            String ruleString = StringArgumentType.getString(context, "rule");
            CarpetRule<?> rule = RuleSelfManager.RULES.get(ruleString);
            if (rule == null) {
                throw CommandUtils.createException("carpet.commands.orange.ruleself.failed");
            }
            boolean value = BoolArgumentType.getBool(context, "value");
            ruleSelfManager.setEnabled(player, ruleString, value);
            Text ruleName = RuleUtils.simpleTranslationName(rule);
            Text playerName = player == CommandUtils.getSourcePlayer(context) ? TextProvider.SELF : player.getDisplayName();
            TextBuilder builder;
            if (value) {
                builder = TextBuilder.of("carpet.commands.orange.ruleself.enable", ruleName, playerName);
            } else {
                builder = TextBuilder.of("carpet.commands.orange.ruleself.disable", ruleName, playerName);
            }
            if (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.isServerDecision()) {
                builder.setHover("carpet.commands.orange.ruleself.invalid");
                builder.setStrikethrough();
            }
            MessageUtils.sendMessage(context.getSource(), builder.build());
            return 1;
        }
        throw CommandUtils.createSelfOrFakePlayerException();
    }

    private int infoRuleSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getArgumentPlayer(context);
        if (CommandUtils.isSelfOrFakePlayer(player, context)) {
            RuleSelfManager ruleSelfManager = GenericFetcherUtils.getRuleSelfManager(player);
            String ruleString = StringArgumentType.getString(context, "rule");
            CarpetRule<?> rule = RuleSelfManager.RULES.get(ruleString);
            if (rule == null) {
                throw CommandUtils.createException("carpet.commands.orange.ruleself.failed");
            }
            MessageUtils.sendEmptyMessage(context);
            MessageUtils.sendMessage(context, "carpet.commands.orange.ruleself.info.player", player.getDisplayName());
            boolean enabled = ruleSelfManager.isEnabled(player, ruleString);
            Text displayName = RuleUtils.simpleTranslationName(rule);
            MessageUtils.sendMessage(context, "carpet.commands.orange.ruleself.info.rule", displayName);
            TextBuilder builder = TextBuilder.of("carpet.commands.orange.ruleself.info.enable", TextProvider.getBoolean(enabled));
            if (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory.isServerDecision()) {
                builder.setHover("carpet.commands.orange.ruleself.invalid");
                builder.setStrikethrough();
            }
            MessageUtils.sendMessage(context.getSource(), builder.build());
            return 1;
        }
        throw CommandUtils.createSelfOrFakePlayerException();
    }

    private int pageTurning(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int id = IntegerArgumentType.getInteger(context, "id");
        int page = IntegerArgumentType.getInteger(context, "page");
        MinecraftServer server = context.getSource().getServer();
        PageManager manager = GenericFetcherUtils.getPageManager(server);
        Optional<PagedCollection> optional = manager.get(id);
        if (optional.isPresent()) {
            PagedCollection collection = optional.get();
            collection.print(page, true);
            return page;
        } else {
            throw CommandUtils.createException("carpet.command.page.non_existent");
        }
    }

    private int openPlayerInventory(CommandContext<ServerCommandSource> context, boolean isInventory) throws CommandSyntaxException {
        UUID uuid = UuidArgumentType.getUuid(context, "uuid");
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            throw CommandUtils.createException("carpet.commands.orange.textclickevent.openInventory.fail");
        }
        Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(uuid, server);
        if (optional.isEmpty()) {
            throw PlayerCommandExtension.createNoFileFoundException();
        }
        GameProfile gameProfile = optional.get();
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        if (isInventory) {
            PlayerCommandExtension.openOfflinePlayerInventory(gameProfile.getName(), server, sourcePlayer, source, gameProfile);
        } else {
            PlayerCommandExtension.openOfflinePlayerEnderChest(gameProfile.getName(), server, sourcePlayer, source, gameProfile);
        }
        return 1;
    }

    @Override
    public String getDefaultName() {
        return "orange";
    }
}
