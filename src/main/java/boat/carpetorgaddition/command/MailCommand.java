package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.exception.CommandExecuteIOException;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.express.Express;
import boat.carpetorgaddition.periodic.express.ExpressManager;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.screen.ShipExpressScreenHandler;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MailCommand extends AbstractServerCommand {
    private final Predicate<CommandSourceStack> intercept = PermissionManager.register("mail.intercept", PermissionLevel.OPS);
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("mail");
    public static final LocalizationKey SEND = KEY.then("send");
    public static final LocalizationKey COLLECT = KEY.then("collect");
    public static final LocalizationKey RECALL = KEY.then("recall");
    public static final LocalizationKey INTERCEPT = KEY.then("intercept");
    public static final LocalizationKey NOTICE = KEY.then("notice");
    public static final LocalizationKey ACTION = KEY.then("action");
    public static final LocalizationKey VERSION = KEY.then("version");
    public static final LocalizationKey LIST = KEY.then("list");

    public MailCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandMail))
                // TODO 重命名为send
                .then(Commands.literal("ship")
                        .then(Commands.argument(CommandUtils.PLAYER, GameProfileArgument.gameProfile())
                                .executes(this::ship)))
                // TODO 重命名为collect
                .then(Commands.literal("receive")
                        .executes(this::receiveAll)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(receiveSuggests(true))
                                .executes(context -> receive(context, false))
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                        .executes(context -> receive(context, BoolArgumentType.getBool(context, "force"))))))
                // TODO 重命名为recall
                .then(Commands.literal("cancel")
                        .executes(this::cancelAll)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(receiveSuggests(false))
                                .executes(context -> cancel(context, false))
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                        .executes(context -> cancel(context, BoolArgumentType.getBool(context, "force"))))))
                // TODO 快递可以同时接收，同时也可以拦截，可接收的物品不应该可以拦截
                .then(Commands.literal("intercept")
                        .requires(intercept)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(interceptSuggests())
                                .executes(context -> intercept(context, false))
                                .then(Commands.argument("force", BoolArgumentType.bool())
                                        .executes(context -> intercept(context, BoolArgumentType.getBool(context, "force"))))))
                .then(Commands.literal("list")
                        .executes(this::list))
                .then(Commands.literal("multiple")
                        .then(Commands.argument(CommandUtils.PLAYER, GameProfileArgument.gameProfile())
                                .executes(this::shipMultipleExpress))));
    }

    // 自动补全快递单号
    private SuggestionProvider<CommandSourceStack> receiveSuggests(boolean recipient) {
        return (context, builder) -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                return SharedSuggestionProvider.suggest(List.of(), builder);
            }
            ExpressManager manager = ServerComponentCoordinator.getCoordinator(context).getExpressManager();
            // 获取所有发送给自己的快递（或所有自己发送的快递）
            List<String> list = manager.stream()
                    .filter(express -> recipient ? express.isRecipient(player) : express.isSender(player))
                    .map(express -> Integer.toString(express.getId()))
                    .toList();
            return SharedSuggestionProvider.suggest(list, builder);
        };
    }

    // 自动补全快递单号
    private SuggestionProvider<CommandSourceStack> interceptSuggests() {
        return (context, builder) -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                return SharedSuggestionProvider.suggest(List.of(), builder);
            }
            ExpressManager manager = ServerComponentCoordinator.getCoordinator(context).getExpressManager();
            // 获取所有发送的快递
            List<String> list = manager.stream()
                    .map(express -> Integer.toString(express.getId()))
                    .toList();
            return SharedSuggestionProvider.suggest(list, builder);
        };
    }

    // 发送快递
    private int ship(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer sourcePlayer = CommandUtils.getSourcePlayer(context);
        GameProfile gameProfile = CommandUtils.getGameProfile(context, "player");
        Optional<ServerPlayer> optional = GenericUtils.getPlayer(server, gameProfile);
        ExpressManager manager = ServerComponentCoordinator.getCoordinator(context).getExpressManager();
        Express express;
        if (optional.isEmpty()) {
            express = new Express(server, sourcePlayer, gameProfile, manager.generateNumber());
        } else {
            ServerPlayer targetPlayer = optional.get();
            // 限制只允许发送给其他真玩家
            checkPlayer(sourcePlayer, targetPlayer);
            express = new Express(server, sourcePlayer, targetPlayer, manager.generateNumber());
        }
        try {
            // 将快递信息添加到快递管理器
            manager.put(express);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
        return 1;
    }

    // 发送多个快递
    private int shipMultipleExpress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer sourcePlayer = CommandUtils.getSourcePlayer(context);
        GameProfile gameProfile = CommandUtils.getGameProfile(context, "player");
        Optional<ServerPlayer> optional = GenericUtils.getPlayer(server, gameProfile);
        if (optional.isPresent()) {
            checkPlayer(sourcePlayer, optional.get());
        }
        SimpleContainer inventory = new SimpleContainer(27);
        SimpleMenuProvider screen = new SimpleMenuProvider(
                (i, inv, _) -> new ShipExpressScreenHandler(i, inv, sourcePlayer, gameProfile, inventory),
                SEND.then("multiple", "gui").translate()
        );
        sourcePlayer.openMenu(screen);
        return 1;
    }

    // 接收快递
    private int receive(CommandContext<CommandSourceStack> context, boolean force) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取快递
        Express express = getExpress(context);
        int dataVersion = express.getNbtDataVersion();
        if (force || dataVersion == -1 || dataVersion == GenericUtils.CURRENT_DATA_VERSION) {
            // 只能接收发送给自己的快递
            if (express.isRecipient(player)) {
                try {
                    express.receive();
                } catch (IOException e) {
                    throw CommandExecuteIOException.of(e);
                }
                return 1;
            }
            throw CommandUtils.createException(COLLECT.then("not_myself").translate());
        } else {
            Component action = ACTION.then("collect").translate();
            Component button = TextProvider.clickRun(CommandProvider.receiveExpress(express.getId(), true));
            Component message = this.differentVersions(action, dataVersion, button);
            MessageUtils.sendMessage(context.getSource(), message);
            return 0;
        }
    }

    // 接收所有快递
    private int receiveAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        try {
            // TODO 未检查是否已撤回
            return ServerComponentCoordinator.getCoordinator(context).getExpressManager().receiveAll(player);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
    }

    // 撤回快递
    private int cancel(CommandContext<CommandSourceStack> context, boolean force) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        Express express = getExpress(context);
        int dataVersion = express.getNbtDataVersion();
        if (force || dataVersion == -1 || dataVersion == GenericUtils.CURRENT_DATA_VERSION) {
            if (express.isSender(player)) {
                try {
                    express.cancel();
                } catch (IOException e) {
                    throw CommandExecuteIOException.of(e);
                }
                return 1;
            }
            throw CommandUtils.createException(RECALL.then("not_myself").translate());
        } else {
            Component action = ACTION.then("recall").translate();
            Component button = TextProvider.clickRun(CommandProvider.cancelExpress(express.getId(), true));
            Component message = this.differentVersions(action, dataVersion, button);
            MessageUtils.sendMessage(context.getSource(), message);
            return 0;
        }
    }

    // 撤回所有快递
    private int cancelAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        try {
            return ServerComponentCoordinator.getCoordinator(context).getExpressManager().cancelAll(player);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
    }

    /**
     * 拦截一件快递
     */
    private int intercept(CommandContext<CommandSourceStack> context, boolean force) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        Express express = getExpress(context);
        int dataVersion = express.getNbtDataVersion();
        if (force || dataVersion == -1 || dataVersion == GenericUtils.CURRENT_DATA_VERSION) {
            try {
                express.intercept(player);
            } catch (IOException e) {
                IOUtils.loggerError(e);
            }
            return express.getId();
        } else {
            Component action = ACTION.then("intercept").translate();
            Component button = TextProvider.clickRun(CommandProvider.interceptExpress(express.getId(), true));
            Component message = this.differentVersions(action, dataVersion, button);
            MessageUtils.sendMessage(context.getSource(), message);
            return 0;
        }
    }

    private Component differentVersions(Component action, int dataVersion, Component button) {
        Component component = VERSION.then(dataVersion > GenericUtils.CURRENT_DATA_VERSION ? "new" : "old").translate();
        TextBuilder builder = new TextBuilder(component);
        TextJoiner joiner = new TextJoiner();
        joiner.append(VERSION.then("expect").translate(GenericUtils.CURRENT_DATA_VERSION));
        joiner.append(VERSION.then("actual").translate(dataVersion));
        builder.setHover(joiner.join());
        return VERSION.then("fail").translate(action, builder.build(), button, action);
    }

    // 列出快递
    private int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = CommandUtils.getSourcePlayer(context);
        ExpressManager manager = ServerComponentCoordinator.getCoordinator(context).getExpressManager();
        CommandSourceStack source = context.getSource();
        List<Express> list = manager.stream().toList();
        if (list.isEmpty()) {
            // 没有快递被列出
            MessageUtils.sendMessage(context, LIST.then("empty").translate());
            return 0;
        }
        PageManager pageManager = FetcherUtils.getPageManager(source.getServer());
        PagedCollection collection = pageManager.newPagedCollection(source);
        ArrayList<Supplier<Component>> messages = new ArrayList<>();
        for (Express express : list) {
            messages.add(() -> line(player, express));
        }
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(source);
        collection.print();
        return list.size();
    }

    private Component line(ServerPlayer player, Express express) {
        ArrayList<Component> list = new ArrayList<>();
        TextBuilder builder;
        if (express.isRecipient(player)) {
            // TODO 改为[C] Collect
            builder = new TextBuilder("[R]");
            // 点击接收
            builder.setCommand(CommandProvider.receiveExpress(express.getId(), false));
            builder.setColor(ChatFormatting.AQUA);
            list.add(LIST.then("collect").translate());
            list.add(TextBuilder.empty());
        } else if (express.isSender(player)) {
            // TODO 改为[R] Recall
            builder = new TextBuilder("[C]");
            // 点击撤回
            builder.setCommand(CommandProvider.cancelExpress(express.getId(), false));
            builder.setColor(ChatFormatting.AQUA);
            list.add(LIST.then("recall").translate());
            list.add(TextBuilder.empty());
        } else if (intercept.test(player.createCommandSourceStack())) {
            builder = new TextBuilder("[I]");
            // 点击拦截
            builder.setCommand(CommandProvider.interceptExpress(express.getId(), false));
            builder.setColor(ChatFormatting.AQUA);
            list.add(LIST.then("intercept").translate());
            list.add(TextBuilder.empty());
        } else {
            builder = new TextBuilder("[?]");
        }
        list.add(LIST.then("id").translate(express.getId()));
        list.add(LIST.then("sender").translate(express.getSender()));
        list.add(LIST.then("recipient").translate(express.getRecipient()));
        ItemStack itemStack = express.getExpress();
        list.add(LIST.then("item").translate(itemStack.getItem().getName(), itemStack.getCount()));
        list.add(LIST.then("time").translate(express.getTime()));
        // 拼接字符串
        builder.setHover(TextBuilder.joinList(list));
        // TODO 改为单号：%s，物品：%s，然后直接添加按钮
        return LIST.then("each").translate(
                express.getId(),
                itemStack.getDisplayName(),
                express.getSender(),
                express.getRecipient(),
                builder.build()
        );
    }

    // 获取快递
    private Express getExpress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ExpressManager manager = ServerComponentCoordinator.getCoordinator(context).getExpressManager();
        // 获取快递单号
        int id = IntegerArgumentType.getInteger(context, "id");
        // 查找指定单号的快递
        Optional<Express> optional = manager.binarySearch(id);
        if (optional.isEmpty()) {
            throw CommandUtils.createException(KEY.then("invalid_parcel").translate(id));
        }
        return optional.get();
    }

    // 检查玩家是否是自己或假玩家
    private void checkPlayer(ServerPlayer sourcePlayer, ServerPlayer targetPlayer) throws CommandSyntaxException {
        // 允许在开发环境下发送给自己
        if (CarpetOrgAddition.isDebugDevelopment()) {
            return;
        }
        if (sourcePlayer == targetPlayer || targetPlayer instanceof EntityPlayerMPFake) {
            throw CommandUtils.createException(SEND.then("check_player").translate());
        }
    }

    public Predicate<CommandSourceStack> getInterceptPredicate() {
        return this.intercept;
    }

    @Override
    public String getDefaultName() {
        return "mail";
    }
}
