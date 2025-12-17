package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.exception.CommandExecuteIOException;
import boat.carpetorgaddition.rule.CustomRuleControl;
import boat.carpetorgaddition.rule.CustomRuleEntry;
import boat.carpetorgaddition.rule.RuleSelfManager;
import boat.carpetorgaddition.rule.RuleUtils;
import boat.carpetorgaddition.rule.value.OpenPlayerInventory;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.GameProfileCache;
import boat.carpetorgaddition.wheel.TextBuilder;
import boat.carpetorgaddition.wheel.inventory.OfflinePlayerInventory;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.permission.CommandPermission;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.provider.TextProvider;
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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
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

    public OrangeCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
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
        this.dispatcher.register(Commands.literal(name)
                .executes(this::openDialog)
                .then(Commands.literal("permission")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.argument("node", StringArgumentType.string())
                                .suggests(suggestsNode())
                                .then(Commands.argument("level", StringArgumentType.string())
                                        .suggests((_, builder) -> SharedSuggestionProvider.suggest(PermissionLevel.listPermission(), builder))
                                        .executes(this::setLevel))))
                .then(Commands.literal("version")
                        .executes(this::version))
                .then(Commands.literal("ruleself")
                        .then(Commands.argument(CommandUtils.PLAYER, EntityArgument.player())
                                .then(Commands.argument("rule", StringArgumentType.string())
                                        .suggests(suggestRule())
                                        .executes(this::infoRuleSelf)
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(this::setRuleSelf)))))
                .then(Commands.literal("textclickevent")
                        .then(Commands.literal("queryPlayerName")
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .executes(this::queryPlayerName)))
                        .then(Commands.literal("pageturning")
                                .then(Commands.argument("id", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                .executes(this::pageTurning))))
                        .then(Commands.literal("openInventory")
                                .requires(OpenPlayerInventory::isEnable)
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .then(Commands.literal("inventory")
                                                .executes(context -> openPlayerInventory(context, true)))
                                        .then(Commands.literal("enderChest")
                                                .executes(context -> openPlayerInventory(context, false)))))));
    }

    private @NotNull SuggestionProvider<CommandSourceStack> suggestRule() {
        return (_, builder) -> SharedSuggestionProvider.suggest(RuleSelfManager.NAME_TO_RULES.values().stream().map(CarpetRule::name), builder);
    }

    private SuggestionProvider<CommandSourceStack> suggestsNode() {
        return (_, builder) -> SharedSuggestionProvider.suggest(
                PermissionManager.listNode().stream().map(StringArgumentType::escapeIfRequired),
                builder
        );
    }

    private int openDialog(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        DialogProvider provider = FetcherUtils.getDialogProvider(server);
        Dialog dialog = provider.getDialog(DialogProvider.START);
        player.openDialog(Holder.direct(dialog));
        return 1;
    }

    // 设置子命令权限
    private int setLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
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
    private int version(CommandContext<CommandSourceStack> context) {
        String name = CarpetOrgAddition.MOD_NAME;
        Component version = new TextBuilder(CarpetOrgAddition.VERSION).setHover(CarpetOrgAddition.BUILD_TIMESTAMP).build();
        MessageUtils.sendMessage(context, "carpet.commands.orange.version", name, version);
        return 1;
    }

    private int queryPlayerName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            UUID uuid = UuidArgument.getUuid(context, "uuid");
            GameProfileCache cache = GameProfileCache.getInstance();
            Optional<String> optional = cache.get(uuid);
            if (optional.isPresent()) {
                // 如果本地存在，就不再从Mojang API获取
                String playerUuid = uuid.toString();
                String playerName = optional.get();
                sendFeekback(context, playerUuid, playerName);
            } else {
                // 本地不存在，从Mojang API获取
                QUERY_PLAYER_NAME_THREAD_POOL.submit(() -> queryPlayerName(context, uuid));
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
    private void queryPlayerName(CommandContext<CommandSourceStack> context, UUID uuid) {
        String name;
        try {
            name = queryPlayerNameFromMojangApi(uuid);
        } catch (CommandSyntaxException e) {
            context.getSource().handleError(e, false, null);
            return;
        }
        GameProfileCache cache = GameProfileCache.getInstance();
        cache.put(uuid, name);
        MinecraftServer server = context.getSource().getServer();
        // 在服务器线程发送命令反馈
        server.execute(() -> sendFeekback(context, uuid.toString(), name));
    }

    private void sendFeekback(CommandContext<CommandSourceStack> context, String playerUuid, String playerName) {
        Component uuid = new TextBuilder(playerUuid).setCopyToClipboard(playerUuid).setColor(ChatFormatting.GRAY).build();
        Component name = new TextBuilder(playerName).setCopyToClipboard(playerName).setColor(ChatFormatting.GRAY).build();
        MessageUtils.sendMessage(context, "carpet.commands.orange.textclickevent.queryPlayerName.success", uuid, name);
    }

    /**
     * 通过Mojang API查询玩家名称
     */
    private String queryPlayerNameFromMojangApi(UUID uuid) throws CommandSyntaxException {
        URL url;
        try {
            URI uri = new URI(GameProfileCache.MOJANG_API.formatted(uuid.toString()));
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
    private int setRuleSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getArgumentPlayer(context);
        if (CommandUtils.isSelfOrFakePlayer(player, context)) {
            RuleSelfManager ruleSelfManager = FetcherUtils.getRuleSelfManager(player);
            String ruleString = StringArgumentType.getString(context, "rule");
            Optional<CustomRuleEntry> optional = RuleSelfManager.get(ruleString);
            if (optional.isEmpty()) {
                throw CommandUtils.createException("carpet.commands.orange.ruleself.failed");
            }
            CustomRuleEntry entry = optional.get();
            CustomRuleControl<?> control = entry.getControl();
            boolean value = BoolArgumentType.getBool(context, "value");
            ruleSelfManager.setEnabled(player, ruleString, value);
            Component ruleName = RuleUtils.simpleTranslationName(entry.getRule());
            Component playerName = (player == CommandUtils.getSourcePlayer(context) ? TextProvider.SELF : player.getDisplayName());
            TextBuilder builder;
            if (value) {
                builder = TextBuilder.of("carpet.commands.orange.ruleself.enable", ruleName, playerName);
            } else {
                builder = TextBuilder.of("carpet.commands.orange.ruleself.disable", ruleName, playerName);
            }
            if (control.isServerDecision()) {
                builder.setHover("carpet.commands.orange.ruleself.invalid");
                builder.setStrikethrough();
            }
            MessageUtils.sendMessage(context.getSource(), builder.build());
            return 1;
        }
        throw CommandUtils.createSelfOrFakePlayerException();
    }

    private int infoRuleSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getArgumentPlayer(context);
        if (CommandUtils.isSelfOrFakePlayer(player, context)) {
            RuleSelfManager ruleSelfManager = FetcherUtils.getRuleSelfManager(player);
            String ruleString = StringArgumentType.getString(context, "rule");
            Optional<CustomRuleEntry> optional = RuleSelfManager.get(ruleString);
            if (optional.isEmpty()) {
                throw CommandUtils.createException("carpet.commands.orange.ruleself.failed");
            }
            CustomRuleEntry entry = optional.get();
            CustomRuleControl<?> control = entry.getControl();
            MessageUtils.sendEmptyMessage(context);
            MessageUtils.sendMessage(context, "carpet.commands.orange.ruleself.info.player", player.getDisplayName());
            boolean enabled = ruleSelfManager.isEnabled(player, ruleString);
            Component displayName = RuleUtils.simpleTranslationName(entry.getRule());
            MessageUtils.sendMessage(context, "carpet.commands.orange.ruleself.info.rule", displayName);
            TextBuilder builder = TextBuilder.of("carpet.commands.orange.ruleself.info.enable", TextProvider.getBoolean(enabled));
            if (control.isServerDecision()) {
                builder.setHover("carpet.commands.orange.ruleself.invalid");
                builder.setStrikethrough();
            }
            MessageUtils.sendMessage(context.getSource(), builder.build());
            return 1;
        }
        throw CommandUtils.createSelfOrFakePlayerException();
    }

    private int pageTurning(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int id = IntegerArgumentType.getInteger(context, "id");
        int page = IntegerArgumentType.getInteger(context, "page");
        MinecraftServer server = context.getSource().getServer();
        PageManager manager = FetcherUtils.getPageManager(server);
        Optional<PagedCollection> optional = manager.get(id);
        if (optional.isPresent()) {
            PagedCollection collection = optional.get();
            collection.print(page, true);
            return page;
        } else {
            throw CommandUtils.createException("carpet.command.page.non_existent");
        }
    }

    private int openPlayerInventory(CommandContext<CommandSourceStack> context, boolean isInventory) throws CommandSyntaxException {
        UUID uuid = UuidArgument.getUuid(context, "uuid");
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            throw CommandUtils.createException("carpet.commands.orange.textclickevent.openInventory.fail");
        }
        if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get().canOpenOfflinePlayer()) {
            Optional<GameProfile> optional = OfflinePlayerInventory.getPlayerConfigEntry(uuid, server).map(entry -> new GameProfile(entry.id(), entry.name()));
            if (optional.isEmpty()) {
                throw PlayerCommandExtension.createNoFileFoundException();
            }
            GameProfile gameProfile = optional.get();
            ServerPlayer sourcePlayer = CommandUtils.getSourcePlayer(context);
            if (isInventory) {
                PlayerCommandExtension.openOfflinePlayerInventory(sourcePlayer, gameProfile);
            } else {
                PlayerCommandExtension.openOfflinePlayerEnderChest(sourcePlayer, gameProfile);
            }
            return 1;
        } else {
            throw CommandUtils.createPlayerNotFoundException();
        }
    }

    @Override
    public String getDefaultName() {
        return "orange";
    }
}
