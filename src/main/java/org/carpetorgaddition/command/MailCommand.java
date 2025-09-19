package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.CommandExecuteIOException;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.express.Express;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.page.PageManager;
import org.carpetorgaddition.wheel.page.PagedCollection;
import org.carpetorgaddition.wheel.permission.PermissionLevel;
import org.carpetorgaddition.wheel.permission.PermissionManager;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.carpetorgaddition.wheel.screen.ShipExpressScreenHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MailCommand extends AbstractServerCommand {
    public final Predicate<ServerCommandSource> intercept = PermissionManager.register("mail.intercept", PermissionLevel.OPS);

    public MailCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandMail))
                .then(CommandManager.literal("ship")
                        .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                .executes(this::ship)))
                .then(CommandManager.literal("receive")
                        .executes(this::receiveAll)
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .suggests(receiveSuggests(true))
                                .executes(this::receive)))
                .then(CommandManager.literal("cancel")
                        .executes(this::cancelAll)
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .suggests(receiveSuggests(false))
                                .executes(this::cancel)))
                .then(CommandManager.literal("intercept")
                        .requires(intercept)
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .suggests(interceptSuggests())
                                .executes(this::intercept)))
                .then(CommandManager.literal("list")
                        .executes(this::list))
                .then(CommandManager.literal("multiple")
                        .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                .executes(this::shipMultipleExpress))));
    }

    // 自动补全快递单号
    private SuggestionProvider<ServerCommandSource> receiveSuggests(boolean recipient) {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                return CommandSource.suggestMatching(List.of(), builder);
            }
            ExpressManager manager = ServerComponentCoordinator.getManager(context).getExpressManager();
            // 获取所有发送给自己的快递（或所有自己发送的快递）
            List<String> list = manager.stream()
                    .filter(express -> recipient ? express.isRecipient(player) : express.isSender(player))
                    .map(express -> Integer.toString(express.getId()))
                    .toList();
            return CommandSource.suggestMatching(list, builder);
        };
    }

    // 自动补全快递单号
    private SuggestionProvider<ServerCommandSource> interceptSuggests() {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                return CommandSource.suggestMatching(List.of(), builder);
            }
            ExpressManager manager = ServerComponentCoordinator.getManager(context).getExpressManager();
            // 获取所有发送的快递
            List<String> list = manager.stream()
                    .map(express -> Integer.toString(express.getId()))
                    .toList();
            return CommandSource.suggestMatching(list, builder);
        };
    }

    // 发送快递
    private int ship(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity targetPlayer = CommandUtils.getArgumentPlayer(context);
        // 限制只允许发送给其他真玩家
        checkPlayer(sourcePlayer, targetPlayer);
        MinecraftServer server = context.getSource().getServer();
        ExpressManager manager = ServerComponentCoordinator.getManager(context).getExpressManager();
        try {
            // 将快递信息添加到快递管理器
            manager.put(new Express(server, sourcePlayer, targetPlayer, manager.generateNumber()));
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
        return 1;
    }

    // 发送多个快递
    private int shipMultipleExpress(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity targetPlayer = CommandUtils.getArgumentPlayer(context);
        checkPlayer(sourcePlayer, targetPlayer);
        SimpleInventory inventory = new SimpleInventory(27);
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((i, inv, player)
                -> new ShipExpressScreenHandler(i, inv, sourcePlayer, targetPlayer, inventory),
                TextBuilder.translate("carpet.commands.mail.multiple.gui"));
        sourcePlayer.openHandledScreen(screen);
        return 1;
    }

    // 接收快递
    private int receive(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取快递
        Express express = getExpress(context);
        // 只能接收发送给自己的快递
        if (express.isRecipient(player)) {
            try {
                express.receive();
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
            return 1;
        }
        throw CommandUtils.createException("carpet.commands.mail.receive.recipient");
    }

    // 接收所有快递
    private int receiveAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        try {
            return ServerComponentCoordinator.getManager(context).getExpressManager().receiveAll(player);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
    }

    // 撤回快递
    private int cancel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        Express express = getExpress(context);
        if (express.isSender(player)) {
            try {
                express.cancel();
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
            return 1;
        }
        throw CommandUtils.createException("carpet.commands.mail.cancel.recipient");
    }

    // 撤回所有快递
    private int cancelAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        try {
            return ServerComponentCoordinator.getManager(context).getExpressManager().cancelAll(player);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
    }

    /**
     * 拦截一件快递
     */
    private int intercept(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        Express express = getExpress(context);
        try {
            express.intercept(player);
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
        return express.getId();
    }

    // 列出快递
    private int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ExpressManager manager = ServerComponentCoordinator.getManager(context).getExpressManager();
        ServerCommandSource source = context.getSource();
        List<Express> list = manager.stream().toList();
        if (list.isEmpty()) {
            // 没有快递被列出
            MessageUtils.sendMessage(context, "carpet.commands.mail.list.empty");
            return 0;
        }
        PageManager pageManager = FetcherUtils.getPageManager(source.getServer());
        PagedCollection collection = pageManager.newPagedCollection(source);
        ArrayList<Supplier<Text>> messages = new ArrayList<>();
        for (Express express : list) {
            messages.add(() -> line(player, express));
        }
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(source);
        collection.print();
        return list.size();
    }

    private Text line(ServerPlayerEntity player, Express express) {
        ArrayList<Text> list = new ArrayList<>();
        TextBuilder builder;
        if (express.isRecipient(player)) {
            builder = new TextBuilder("[R]");
            // 点击接收
            builder.setCommand(CommandProvider.receiveExpress(express.getId()));
            builder.setColor(Formatting.AQUA);
            list.add(TextBuilder.translate("carpet.commands.mail.list.receive"));
            list.add(TextBuilder.empty());
        } else if (express.isSender(player)) {
            builder = new TextBuilder("[C]");
            // 点击撤回
            builder.setCommand(CommandProvider.cancelExpress(express.getId()));
            builder.setColor(Formatting.AQUA);
            list.add(TextBuilder.translate("carpet.commands.mail.list.sending"));
            list.add(TextBuilder.empty());
        } else if (intercept.test(player.getCommandSource())) {
            builder = new TextBuilder("[I]");
            // 点击拦截
            builder.setCommand(CommandProvider.interceptExpress(express.getId()));
            builder.setColor(Formatting.AQUA);
            list.add(TextBuilder.translate("carpet.commands.mail.list.intercept"));
            list.add(TextBuilder.empty());
        } else {
            builder = new TextBuilder("[?]");
        }
        list.add(TextBuilder.translate("carpet.commands.mail.list.id", express.getId()));
        list.add(TextBuilder.translate("carpet.commands.mail.list.sender", express.getSender()));
        list.add(TextBuilder.translate("carpet.commands.mail.list.recipient", express.getRecipient()));
        list.add(TextBuilder.translate("carpet.commands.mail.list.item",
                TextBuilder.translate(express.getExpress().getTranslationKey()), express.getExpress().getCount()));
        list.add(TextBuilder.translate("carpet.commands.mail.list.time", express.getTime()));
        // 拼接字符串
        builder.setHover(TextBuilder.joinList(list));
        return TextBuilder.translate(
                "carpet.commands.mail.list.each",
                express.getId(),
                express.getExpress().toHoverableText(),
                express.getSender(),
                express.getRecipient(),
                builder.build()
        );
    }

    // 获取快递
    private Express getExpress(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ExpressManager manager = ServerComponentCoordinator.getManager(context).getExpressManager();
        // 获取快递单号
        int id = IntegerArgumentType.getInteger(context, "id");
        // 查找指定单号的快递
        Optional<Express> optional = manager.binarySearch(id);
        if (optional.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.mail.receive.non_existent", id);
        }
        return optional.get();
    }

    // 检查玩家是否是自己或假玩家
    private void checkPlayer(ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer) throws CommandSyntaxException {
        // 允许在开发环境下发送给自己
        if (CarpetOrgAddition.isDebugDevelopment()) {
            return;
        }
        if (sourcePlayer == targetPlayer || targetPlayer instanceof EntityPlayerMPFake) {
            throw CommandUtils.createException("carpet.commands.mail.check_player");
        }
    }

    @Override
    public String getDefaultName() {
        return "mail";
    }
}
