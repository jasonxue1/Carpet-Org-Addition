package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.exception.CommandExecuteIOException;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.parcel.Parcel;
import boat.carpetorgaddition.periodic.parcel.ParcelManager;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.permission.PermissionLevel;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.screen.SendExpressScreenHandler;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
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
    public static final LocalizationKey LIST = KEY.then("list");

    public MailCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandMail))
                .then(Commands.literal("send")
                        .then(Commands.argument(CommandUtils.PLAYER, GameProfileArgument.gameProfile())
                                .executes(this::send)))
                .then(Commands.literal("collect")
                        .executes(this::collectAll)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(collectSuggests(true))
                                .executes(this::collect)))
                .then(Commands.literal("recall")
                        .executes(this::recallAll)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(collectSuggests(false))
                                .executes(this::recall)))
                // TODO 快递可以同时接收，同时也可以拦截，可接收的物品不应该可以拦截
                .then(Commands.literal("intercept")
                        .requires(intercept)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .suggests(interceptSuggests())
                                .executes(this::intercept)))
                .then(Commands.literal("list")
                        .executes(this::list))
                .then(Commands.literal("multiple")
                        .then(Commands.argument(CommandUtils.PLAYER, GameProfileArgument.gameProfile())
                                .executes(this::sendMultipleExpress)))
                .then(Commands.literal("override")
                        .requires(_ -> CarpetOrgAddition.isDebugDevelopment())
                        .executes(this::override)));
    }

    // 自动补全快递单号
    private SuggestionProvider<CommandSourceStack> collectSuggests(boolean recipient) {
        return (context, builder) -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                return SharedSuggestionProvider.suggest(List.of(), builder);
            }
            ParcelManager manager = ServerComponentCoordinator.getCoordinator(context).getParcelManager();
            // 获取所有发送给自己的快递（或所有自己发送的快递）
            List<String> list = manager.stream()
                    .filter(parcel -> recipient ? parcel.isRecipient(player) : parcel.isSender(player))
                    .map(parcel -> Integer.toString(parcel.getId()))
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
            ParcelManager manager = ServerComponentCoordinator.getCoordinator(context).getParcelManager();
            // 获取所有发送的快递
            List<String> list = manager.stream()
                    .map(parcel -> Integer.toString(parcel.getId()))
                    .toList();
            return SharedSuggestionProvider.suggest(list, builder);
        };
    }

    // 发送快递
    private int send(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer sourcePlayer = CommandUtils.getSourcePlayer(context);
        GameProfile gameProfile = CommandUtils.getGameProfile(context, "player");
        Optional<ServerPlayer> optional = GenericUtils.getPlayer(server, gameProfile);
        ParcelManager manager = ServerComponentCoordinator.getCoordinator(context).getParcelManager();
        Parcel parcel;
        if (optional.isEmpty()) {
            parcel = new Parcel(server, sourcePlayer, gameProfile, manager.generateNumber());
        } else {
            ServerPlayer targetPlayer = optional.get();
            // 限制只允许发送给其他真玩家
            checkPlayer(sourcePlayer, targetPlayer);
            parcel = new Parcel(server, sourcePlayer, targetPlayer, manager.generateNumber());
        }
        try {
            // 将快递信息添加到快递管理器
            manager.put(parcel);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
        return 1;
    }

    // 发送多个快递
    private int sendMultipleExpress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer sourcePlayer = CommandUtils.getSourcePlayer(context);
        GameProfile gameProfile = CommandUtils.getGameProfile(context, "player");
        Optional<ServerPlayer> optional = GenericUtils.getPlayer(server, gameProfile);
        if (optional.isPresent()) {
            checkPlayer(sourcePlayer, optional.get());
        }
        SimpleContainer inventory = new SimpleContainer(27);
        SimpleMenuProvider screen = new SimpleMenuProvider(
                (i, inv, _) -> new SendExpressScreenHandler(i, inv, sourcePlayer, gameProfile, inventory),
                SEND.then("multiple", "gui").translate()
        );
        sourcePlayer.openMenu(screen);
        return 1;
    }

    // 接收快递
    private int collect(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取快递
        Parcel parcel = getExpress(context);
        // 只能接收发送给自己的快递
        if (parcel.isRecipient(player)) {
            try {
                parcel.collect();
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
            return 1;
        }
        throw CommandUtils.createException(COLLECT.then("not_myself").translate());
    }

    // 接收所有快递
    private int collectAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        try {
            // TODO 未检查是否已撤回
            return ServerComponentCoordinator.getCoordinator(context).getParcelManager().collectAll(player);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
    }

    // 撤回快递
    private int recall(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        Parcel parcel = getExpress(context);
        if (parcel.isSender(player)) {
            try {
                parcel.recall();
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
            return 1;
        }
        throw CommandUtils.createException(RECALL.then("not_myself").translate());
    }

    // 撤回所有快递
    private int recallAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        try {
            return ServerComponentCoordinator.getCoordinator(context).getParcelManager().recallAll(player);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
    }

    /**
     * 拦截一件快递
     */
    private int intercept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        Parcel parcel = getExpress(context);
        try {
            parcel.intercept(player);
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
        return parcel.getId();
    }

    // 调试：将快递数据写入文件
    private int override(CommandContext<CommandSourceStack> context) {
        if (CarpetOrgAddition.isDebugDevelopment()) {
            ParcelManager manager = ServerComponentCoordinator.getCoordinator(context).getParcelManager();
            manager.stream().forEach(parcel -> {
                try {
                    parcel.save();
                } catch (IOException e) {
                    IOUtils.loggerError(e);
                }
            });
            return (int) manager.stream().count();
        }
        return 0;
    }

    // 列出快递
    private int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        final ServerPlayer player = CommandUtils.getSourcePlayer(context);
        ParcelManager manager = ServerComponentCoordinator.getCoordinator(context).getParcelManager();
        CommandSourceStack source = context.getSource();
        List<Parcel> list = manager.stream().toList();
        if (list.isEmpty()) {
            // 没有快递被列出
            MessageUtils.sendMessage(context, LIST.then("empty").translate());
            return 0;
        }
        PageManager pageManager = FetcherUtils.getPageManager(source.getServer());
        PagedCollection collection = pageManager.newPagedCollection(source);
        ArrayList<Supplier<Component>> messages = new ArrayList<>();
        for (Parcel parcel : list) {
            messages.add(() -> line(player, parcel));
        }
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(source);
        collection.print();
        return list.size();
    }

    private Component line(ServerPlayer player, Parcel parcel) {
        ArrayList<Component> list = new ArrayList<>();
        TextBuilder builder;
        if (parcel.isRecipient(player)) {
            // TODO 改为[C] Collect
            builder = new TextBuilder("[R]");
            // 点击接收
            builder.setCommand(CommandProvider.collectExpress(parcel.getId(), false));
            builder.setColor(ChatFormatting.AQUA);
            list.add(LIST.then("collect").translate());
            list.add(TextBuilder.empty());
        } else if (parcel.isSender(player)) {
            // TODO 改为[R] Recall
            builder = new TextBuilder("[C]");
            // 点击撤回
            builder.setCommand(CommandProvider.recallExpress(parcel.getId(), false));
            builder.setColor(ChatFormatting.AQUA);
            list.add(LIST.then("recall").translate());
            list.add(TextBuilder.empty());
        } else if (intercept.test(player.createCommandSourceStack())) {
            builder = new TextBuilder("[I]");
            // 点击拦截
            builder.setCommand(CommandProvider.interceptExpress(parcel.getId(), false));
            builder.setColor(ChatFormatting.AQUA);
            list.add(LIST.then("intercept").translate());
            list.add(TextBuilder.empty());
        } else {
            builder = new TextBuilder("[?]");
        }
        list.add(LIST.then("id").translate(parcel.getId()));
        list.add(LIST.then("sender").translate(parcel.getSender()));
        list.add(LIST.then("recipient").translate(parcel.getRecipient()));
        ItemStack itemStack = parcel.getExpress();
        list.add(LIST.then("item").translate(itemStack.getItem().getName(), itemStack.getCount()));
        list.add(LIST.then("time").translate(parcel.getTime()));
        // 拼接字符串
        builder.setHover(TextBuilder.joinList(list));
        // TODO 改为单号：%s，物品：%s，然后直接添加按钮
        return LIST.then("each").translate(
                parcel.getId(),
                itemStack.getDisplayName(),
                parcel.getSender(),
                parcel.getRecipient(),
                builder.build()
        );
    }

    // 获取快递
    private Parcel getExpress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ParcelManager manager = ServerComponentCoordinator.getCoordinator(context).getParcelManager();
        // 获取快递单号
        int id = IntegerArgumentType.getInteger(context, "id");
        // 查找指定单号的快递
        Optional<Parcel> optional = manager.binarySearch(id);
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
