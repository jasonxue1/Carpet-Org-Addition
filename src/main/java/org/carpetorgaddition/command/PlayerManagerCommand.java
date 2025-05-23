package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
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
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSafeAfkInterface;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import org.carpetorgaddition.periodic.task.ServerTaskManager;
import org.carpetorgaddition.periodic.task.schedule.DelayedLoginTask;
import org.carpetorgaddition.periodic.task.schedule.DelayedLogoutTask;
import org.carpetorgaddition.periodic.task.schedule.PlayerScheduleTask;
import org.carpetorgaddition.periodic.task.schedule.ReLoginTask;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.GenericFetcherUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.page.PageManager;
import org.carpetorgaddition.util.page.PagedCollection;
import org.carpetorgaddition.util.permission.PermissionLevel;
import org.carpetorgaddition.util.permission.PermissionManager;
import org.carpetorgaddition.util.provider.CommandProvider;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.TextBuilder;
import org.carpetorgaddition.util.wheel.WorldFormat;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

// TODO 玩家分组
public class PlayerManagerCommand extends AbstractServerCommand {
    private static final String SAFEAFK_PROPERTIES = "safeafk.properties";

    public PlayerManagerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        // 延迟登录节点
        RequiredArgumentBuilder<ServerCommandSource, Integer> loginNode = CommandManager.argument("delayed", IntegerArgumentType.integer(1));
        for (TimeUnit unit : TimeUnit.values()) {
            // 添加时间单位
            loginNode.then(CommandManager.literal(unit.getName())
                    .executes(context -> addDelayedLoginTask(context, unit)));
        }
        // 延迟登出节点
        RequiredArgumentBuilder<ServerCommandSource, Integer> logoutNode = CommandManager.argument("delayed", IntegerArgumentType.integer(1));
        for (TimeUnit unit : TimeUnit.values()) {
            logoutNode.then(CommandManager.literal(unit.getName())
                    .executes(context -> addDelayedLogoutTask(context, unit)));
        }
        this.dispatcher.register(CommandManager.literal(name)
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandPlayerManager))
                .then(CommandManager.literal("save")
                        .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                .executes(context -> savePlayerData(context, false))
                                .then(CommandManager.argument("comment", StringArgumentType.string())
                                        .executes(context -> savePlayerData(context, true)))))
                .then(CommandManager.literal("spawn")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .executes(this::spawnPlayer)))
                .then(CommandManager.literal("modify")
                        .then(CommandManager.literal("comment")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(defaultSuggests())
                                        .executes(context -> setComment(context, true))
                                        .then(CommandManager.argument("comment", StringArgumentType.string())
                                                .executes(context -> setComment(context, false)))))
                        .then(CommandManager.literal("resave")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .executes(this::modifyPlayer))))
                .then(CommandManager.literal("autologin")
                        .requires(PermissionManager.register("playerManager.autologin", PermissionLevel.PASS))
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .then(CommandManager.argument("autologin", BoolArgumentType.bool())
                                        .executes(this::setAutoLogin))))
                .then(CommandManager.literal("list")
                        .executes(context -> list(context, s -> true))
                        .then(CommandManager.argument("filter", StringArgumentType.string())
                                .executes(context -> list(context, s -> s.contains(StringArgumentType.getString(context, "filter").toLowerCase(Locale.ROOT))))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .executes(this::delete)))
                .then(CommandManager.literal("schedule")
                        .then(CommandManager.literal("relogin")
                                .requires(PermissionManager.register("playerManager.schedule.relogin", PermissionLevel.PASS))
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(reLoginTaskSuggests())
                                        .then(CommandManager.argument("interval", IntegerArgumentType.integer(1))
                                                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"1", "3", "5"}, builder))
                                                .executes(this::setReLogin))
                                        .then(CommandManager.literal("stop")
                                                .executes(this::stopReLogin))))
                        .then(CommandManager.literal("login")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(defaultSuggests())
                                        .then(loginNode)))
                        .then(CommandManager.literal("logout")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .then(logoutNode)))
                        .then(CommandManager.literal("cancel")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(cancelSuggests())
                                        .executes(this::cancelScheduleTask)))
                        .then(CommandManager.literal("list")
                                .executes(this::listScheduleTask)))
                .then(CommandManager.literal("safeafk")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .executes(context -> safeAfk(context, 5F, false))
                                        .then(CommandManager.argument("threshold", FloatArgumentType.floatArg())
                                                .executes(context -> safeAfk(context, FloatArgumentType.getFloat(context, "threshold"), false))
                                                .then(CommandManager.argument("save", BoolArgumentType.bool())
                                                        .executes(context -> safeAfk(context, FloatArgumentType.getFloat(context, "threshold"), BoolArgumentType.getBool(context, "save")))))))
                        .then(CommandManager.literal("list")
                                .executes(this::listSafeAfk))
                        .then(CommandManager.literal("cancel")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .executes(context -> cancelSafeAfk(context, false))
                                        .then(CommandManager.argument("save", BoolArgumentType.bool())
                                                .executes(context -> cancelSafeAfk(context, true)))))
                        .then(CommandManager.literal("query")
                                .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                        .executes(this::querySafeAfk)))));
    }

    // cancel子命令自动补全
    @NotNull
    private SuggestionProvider<ServerCommandSource> cancelSuggests() {
        return (context, builder) -> {
            ServerTaskManager manager = ServerComponentCoordinator.getManager(context).getServerTaskManager();
            Stream<String> stream = manager.stream(PlayerScheduleTask.class).map(PlayerScheduleTask::getPlayerName);
            return CommandSource.suggestMatching(stream, builder);
        };
    }

    // 自动补全玩家名
    private SuggestionProvider<ServerCommandSource> defaultSuggests() {
        return (context, builder) -> CommandSource.suggestMatching(new WorldFormat(context.getSource().getServer(),
                FakePlayerSerializer.PLAYER_DATA).toImmutableFileList().stream()
                .filter(file -> file.getName().endsWith(IOUtils.JSON_EXTENSION))
                .map(file -> IOUtils.removeExtension(file.getName(), IOUtils.JSON_EXTENSION))
                .map(StringArgumentType::escapeIfRequired), builder);
    }

    // relogin子命令自动补全
    @NotNull
    private SuggestionProvider<ServerCommandSource> reLoginTaskSuggests() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManager manager = ServerComponentCoordinator.getManager(server).getServerTaskManager();
            // 所有正在周期性上下线的玩家
            List<String> taskList = manager.stream(ReLoginTask.class).map(ReLoginTask::getPlayerName).toList();
            // 所有在线玩家
            List<String> onlineList = server.getPlayerManager()
                    .getPlayerList()
                    .stream()
                    .map(player -> player.getName().getString())
                    .toList();
            HashSet<String> players = new HashSet<>();
            players.addAll(taskList);
            players.addAll(onlineList);
            return CommandSource.suggestMatching(players.stream(), builder);
        };
    }

    // 安全挂机
    private int safeAfk(CommandContext<ServerCommandSource> context, float threshold, boolean save) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 假玩家安全挂机阈值必须小于玩家最大生命值
        if (threshold >= fakePlayer.getMaxHealth()) {
            throw CommandUtils.createException("carpet.commands.playerManager.safeafk.threshold_too_high");
        }
        // 低于或等于0的值没有实际意义，统一设置为-1
        if (threshold <= 0F) {
            threshold = -1F;
        }
        // 设置安全挂机阈值
        FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) fakePlayer;
        safeAfk.setHealthThreshold(threshold);
        if (save) {
            try {
                saveSafeAfkThreshold(context, threshold, fakePlayer);
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
        } else {
            String command = CommandProvider.setupSafeAfkPermanentlyChange(fakePlayer, threshold);
            MessageUtils.sendMessage(
                    context,
                    "carpet.commands.playerManager.safeafk.successfully_set_up",
                    fakePlayer.getDisplayName(),
                    threshold,
                    TextProvider.clickRun(command)
            );
        }
        return (int) threshold;
    }

    // 列出所有设置了安全挂机的在线假玩家
    private int listSafeAfk(CommandContext<ServerCommandSource> context) {
        List<ServerPlayerEntity> list = context.getSource().getServer().getPlayerManager().getPlayerList()
                .stream().filter(player -> player instanceof EntityPlayerMPFake).toList();
        int count = 0;
        // 遍历所有在线并且设置了安全挂机的假玩家
        for (ServerPlayerEntity player : list) {
            float threshold = ((FakePlayerSafeAfkInterface) player).getHealthThreshold();
            if (threshold < 0) {
                continue;
            }
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.list.each",
                    player.getDisplayName(), threshold);
            count++;
        }
        // 没有玩家被列出
        if (count == 0) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.list.empty");
        }
        return count;
    }

    // 取消假玩家的安全挂机
    private int cancelSafeAfk(CommandContext<ServerCommandSource> context, boolean remove) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 设置安全挂机阈值
        FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) fakePlayer;
        safeAfk.setHealthThreshold(-1);
        if (remove) {
            try {
                saveSafeAfkThreshold(context, -1, fakePlayer);
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
        } else {
            String key = "carpet.commands.playerManager.safeafk.successfully_set_up.cancel";
            MutableText command = TextProvider.clickRun(CommandProvider.cancelSafeAfkPermanentlyChange(fakePlayer));
            MessageUtils.sendMessage(context, key, fakePlayer.getDisplayName(), command);
        }
        return 1;
    }

    // 查询指定玩家的安全挂机阈值
    private int querySafeAfk(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        float threshold = ((FakePlayerSafeAfkInterface) fakePlayer).getHealthThreshold();
        String key = "carpet.commands.playerManager.safeafk.list.each";
        MessageUtils.sendMessage(context, key, fakePlayer.getDisplayName(), threshold);
        return (int) threshold;
    }

    // 保存或删除安全挂机阈值
    private void saveSafeAfkThreshold(CommandContext<ServerCommandSource> context, float threshold, EntityPlayerMPFake fakePlayer) throws IOException {
        String playerName = fakePlayer.getName().getString();
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), null);
        File file = worldFormat.file(SAFEAFK_PROPERTIES);
        // 文件存在或者文件成功创建
        if (file.isFile() || file.createNewFile()) {
            Properties properties = new Properties();
            BufferedReader reader = IOUtils.toReader(file);
            try (reader) {
                properties.load(reader);
            }
            if (threshold > 0) {
                // 将玩家安全挂机阈值保存到配置文件
                properties.setProperty(playerName, String.valueOf(threshold));
                MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.successfully_set_up.save", fakePlayer.getDisplayName(), threshold);
            } else {
                // 将玩家安全挂机阈值从配置文件中删除
                properties.remove(playerName);
                MessageUtils.sendMessage(context, "carpet.commands.playerManager.safeafk.successfully_set_up.remove", fakePlayer.getDisplayName());
            }
            BufferedWriter writer = IOUtils.toWriter(file);
            try (writer) {
                properties.store(writer, null);
            }
        }
    }

    /**
     * 加载安全挂机阈值
     */
    public static void loadSafeAfk(ServerPlayerEntity player) {
        if (player instanceof EntityPlayerMPFake) {
            WorldFormat worldFormat = new WorldFormat(player.server, null);
            File file = worldFormat.file(SAFEAFK_PROPERTIES);
            // 文件必须存在
            if (file.isFile()) {
                Properties properties = new Properties();
                try {
                    BufferedReader reader = IOUtils.toReader(file);
                    try (reader) {
                        properties.load(reader);
                    }
                } catch (IOException e) {
                    CarpetOrgAddition.LOGGER.error("假玩家安全挂机阈值加载时出错", e);
                    return;
                }
                try {
                    // 设置安全挂机阈值
                    FakePlayerSafeAfkInterface safeAfk = (FakePlayerSafeAfkInterface) player;
                    String value = properties.getProperty(player.getName().getString());
                    if (value == null) {
                        return;
                    }
                    float threshold = Float.parseFloat(value);
                    safeAfk.setHealthThreshold(threshold);
                    // 广播阈值设置的消息
                    String key = "carpet.commands.playerManager.safeafk.successfully_set_up.auto";
                    TextBuilder builder = TextBuilder.of(key, player.getDisplayName(), threshold);
                    builder.setGrayItalic();
                    MessageUtils.broadcastMessage(player.server, builder.build());
                } catch (NumberFormatException e) {
                    CarpetOrgAddition.LOGGER.error("{}安全挂机阈值设置失败", player.getName().getString(), e);
                }
            }
        }
    }

    // 列出每一个玩家
    private int list(CommandContext<ServerCommandSource> context, Predicate<String> filter) throws CommandSyntaxException {
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerializer.PLAYER_DATA);
        ArrayList<Supplier<Text>> list = FakePlayerSerializer.list(worldFormat, filter);
        if (list.isEmpty()) {
            // 没有玩家被列出
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.list.no_player");
            return 0;
        }
        PageManager pageManager = GenericFetcherUtils.getPageManager(context.getSource().getServer());
        PagedCollection collection = pageManager.newPagedCollection(context.getSource());
        collection.addContent(list);
        if (collection.totalPages() > 1) {
            MessageUtils.sendEmptyMessage(context);
        }
        collection.print();
        return list.size();
    }

    // 设置注释
    private int setComment(CommandContext<ServerCommandSource> context, boolean remove) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerializer.PLAYER_DATA);
        // 修改注释
        String comment = remove ? null : StringArgumentType.getString(context, "comment");
        FakePlayerSerializer serializer = FakePlayerSerializer.load(worldFormat, name);
        serializer.setComment(comment);
        // 将玩家信息重新保存的本地文件
        serializer.save(context.getSource().getServer());
        // 发送命令反馈
        if (remove) {
            // 移除注释
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.comment.remove", serializer.getDisplayName());
        } else {
            // 修改注释
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.comment.modify", serializer.getDisplayName(), comment);
        }
        return 1;
    }

    // 设置自动登录
    private int setAutoLogin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerializer.PLAYER_DATA);
        String name = StringArgumentType.getString(context, "name");
        boolean autologin = BoolArgumentType.getBool(context, "autologin");
        FakePlayerSerializer serial = FakePlayerSerializer.load(worldFormat, name);
        // 设置自动登录
        serial.setAutologin(autologin);
        serial.save(context.getSource().getServer());
        // 发送命令反馈
        if (autologin) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.autologin.setup", serial.getDisplayName());
        } else {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.autologin.cancel", serial.getDisplayName());
        }
        return 1;
    }

    // 保存玩家
    private int savePlayerData(CommandContext<ServerCommandSource> context, boolean hasComment) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        WorldFormat worldFormat = new WorldFormat(server, FakePlayerSerializer.PLAYER_DATA);
        // 玩家数据是否已存在
        String name = fakePlayer.getName().getString();
        boolean exists = worldFormat.hasFile(name, IOUtils.JSON_EXTENSION);
        if (exists) {
            String command = CommandProvider.playerManagerResave(name);
            // 单击执行命令
            MutableText clickResave = TextProvider.clickRun(command);
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.save.file_already_exist", clickResave);
            return 0;
        }
        FakePlayerSerializer serializer;
        if (hasComment) {
            String comment = StringArgumentType.getString(context, "comment");
            serializer = new FakePlayerSerializer(fakePlayer, comment);
        } else {
            serializer = new FakePlayerSerializer(fakePlayer);
        }
        serializer.save(server);
        // 首次保存
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerManager.save.success", fakePlayer.getDisplayName());
        return 1;
    }

    // 修改玩家数据
    private int modifyPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        WorldFormat worldFormat = new WorldFormat(server, FakePlayerSerializer.PLAYER_DATA);
        FakePlayerSerializer serializer = FakePlayerSerializer.load(worldFormat, fakePlayer.getName().getString());
        serializer.setPlayerData(fakePlayer);
        serializer.save(server);
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerManager.save.resave", fakePlayer.getDisplayName());
        return 1;
    }

    // 生成假玩家
    private int spawnPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerializer.PLAYER_DATA);
        try {
            FakePlayerSerializer serial = FakePlayerSerializer.load(worldFormat, name);
            // 生成假玩家
            serial.spawn(context.getSource().getServer());
        } catch (RuntimeException e) {
            // 尝试生成假玩家时出现意外问题
            throw CommandUtils.createException(e, "carpet.commands.playerManager.spawn.fail");
        }
        return 1;
    }

    // 删除玩家信息
    private int delete(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        WorldFormat worldFormat = new WorldFormat(context.getSource().getServer(), FakePlayerSerializer.PLAYER_DATA);
        String name = StringArgumentType.getString(context, "name");
        File file = worldFormat.file(name + IOUtils.JSON_EXTENSION);
        // 文件存在且文件删除成功
        if (file.isFile() && file.delete()) {
            MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerManager.delete.success");
        } else {
            throw CommandUtils.createException("carpet.commands.playerManager.delete.fail");
        }
        return 1;
    }

    // 设置不断重新上线下线
    private int setReLogin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // 强制启用内存泄漏修复
        if (fixMemoryLeak(context)) {
            // 获取目标假玩家名
            String name = StringArgumentType.getString(context, "name");
            int interval = IntegerArgumentType.getInteger(context, "interval");
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManager manager = ServerComponentCoordinator.getManager(context).getServerTaskManager();
            // 如果任务存在，修改任务，否则添加任务
            Optional<ReLoginTask> optional = manager.stream(ReLoginTask.class)
                    .filter(task -> Objects.equals(task.getPlayerName(), name))
                    .findFirst();
            if (optional.isEmpty()) {
                // 添加任务
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
                if (player == null) {
                    // 玩家不存在
                    throw CommandUtils.createException("argument.entity.notfound.player");
                } else {
                    // 目标玩家不是假玩家
                    CommandUtils.assertFakePlayer(player);
                }
                manager.addTask(new ReLoginTask(name, interval, server, player.getServerWorld().getRegistryKey(), context));
            } else {
                // 修改周期时间
                optional.get().setInterval(interval);
                MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.relogin.set_interval", name, interval);
            }
            return interval;
        }
        return 0;
    }

    // 启用内存泄漏修复
    private boolean fixMemoryLeak(CommandContext<ServerCommandSource> context) {
        if (CarpetOrgAdditionSettings.fakePlayerSpawnMemoryLeakFix) {
            return true;
        }
        // 单击后输入的命令
        String command = CommandProvider.setCarpetRule("fakePlayerSpawnMemoryLeakFix", "true");
        // 文本内容：[这里]
        Text here = TextBuilder.of("carpet.command.text.click.here")
                .setSuggest(command)
                .setColor(Formatting.AQUA)
                .setHover("carpet.command.text.click.input", command)
                .build();
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.relogin.condition", here);
        return false;
    }

    // 停止重新上线下线
    private int stopReLogin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // 获取目标假玩家名
        String name = StringArgumentType.getString(context, "name");
        ServerTaskManager manager = ServerComponentCoordinator.getManager(context).getServerTaskManager();
        Optional<ReLoginTask> optional = manager.stream(ReLoginTask.class)
                .filter(task -> Objects.equals(task.getPlayerName(), name))
                .findFirst();
        if (optional.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.playerManager.schedule.cancel.fail");
        }
        optional.ifPresent(task -> task.onCancel(context));
        return 1;
    }

    // 延时上线
    private int addDelayedLoginTask(CommandContext<ServerCommandSource> context, TimeUnit unit) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ServerTaskManager manager = ServerComponentCoordinator.getManager(context).getServerTaskManager();
        String name = StringArgumentType.getString(context, "name");
        Optional<DelayedLoginTask> optional = manager.stream(DelayedLoginTask.class)
                .filter(task -> Objects.equals(name, task.getPlayerName()))
                .findFirst();
        // 等待时间
        long tick = unit.getDelayed(context);
        MutableText time = new TextBuilder(TextProvider.tickToTime(tick)).setHover(TextProvider.tickToRealTime(tick)).build();
        if (optional.isEmpty()) {
            // 添加上线任务
            WorldFormat worldFormat = new WorldFormat(server, FakePlayerSerializer.PLAYER_DATA);
            FakePlayerSerializer serial = FakePlayerSerializer.load(worldFormat, name);
            manager.addTask(new DelayedLoginTask(server, serial, tick));
            String key = server.getPlayerManager().getPlayer(name) == null
                    // <玩家>将于<时间>后上线
                    ? "carpet.commands.playerManager.schedule.login"
                    // <玩家>将于<时间>后再次尝试上线
                    : "carpet.commands.playerManager.schedule.login.try";
            // 发送命令反馈
            MessageUtils.sendMessage(context, key, serial.getDisplayName(), time);
        } else {
            // 修改上线时间
            DelayedLoginTask task = optional.get();
            // 为名称添加悬停文本
            TextBuilder builder = new TextBuilder(name);
            builder.setHover(task.getInfo());
            task.setDelayed(tick);
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.login.modify", builder.build(), time);
        }
        return (int) tick;
    }

    // 延迟下线
    private int addDelayedLogoutTask(CommandContext<ServerCommandSource> context, TimeUnit unit) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 获取假玩家延时下线游戏刻数
        long tick = unit.getDelayed(context);
        MutableText time = new TextBuilder(TextProvider.tickToTime(tick)).setHover(TextProvider.tickToRealTime(tick)).build();
        ServerTaskManager manager = ServerComponentCoordinator.getManager(server).getServerTaskManager();
        Optional<DelayedLogoutTask> optional = manager.stream(DelayedLogoutTask.class)
                .filter(task -> fakePlayer.equals(task.getFakePlayer()))
                .findFirst();
        // 添加新任务
        if (optional.isEmpty()) {
            // 添加延时下线任务
            manager.addTask(new DelayedLogoutTask(server, fakePlayer, tick));
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.logout", fakePlayer.getDisplayName(), time);
        } else {
            // 修改退出时间
            optional.get().setDelayed(tick);
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.logout.modify", fakePlayer.getDisplayName(), time);
        }
        return (int) tick;
    }

    // 取消任务
    private int cancelScheduleTask(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerTaskManager manager = ServerComponentCoordinator.getManager(context).getServerTaskManager();
        String name = StringArgumentType.getString(context, "name");
        // 获取符合条件的任务列表
        List<PlayerScheduleTask> list = manager.stream(PlayerScheduleTask.class)
                .filter(task -> Objects.equals(task.getPlayerName(), name))
                .toList();
        if (list.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.playerManager.schedule.cancel.fail");
        }
        list.forEach(task -> task.onCancel(context));
        return list.size();
    }

    // 列出所有任务
    private int listScheduleTask(CommandContext<ServerCommandSource> context) {
        ServerTaskManager manager = ServerComponentCoordinator.getManager(context).getServerTaskManager();
        List<PlayerScheduleTask> list = manager.stream(PlayerScheduleTask.class).toList();
        if (list.isEmpty()) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.list.empty");
        } else {
            list.forEach(task -> task.sendEachMessage(context.getSource()));
        }
        return list.size();
    }

    @Override
    public String getDefaultName() {
        return "playerManager";
    }

    /**
     * 时间单位
     */
    private enum TimeUnit {
        /**
         * tick
         */
        TICK,
        /**
         * 秒
         */
        SECOND,
        /**
         * 分钟
         */
        MINUTE,
        /**
         * 小时
         */
        HOUR;

        // 获取单位名称
        private String getName() {
            return switch (this) {
                case TICK -> "t";
                case SECOND -> "s";
                case MINUTE -> "min";
                case HOUR -> "h";
            };
        }

        // 将游戏刻转化为对应单位
        private long getDelayed(CommandContext<ServerCommandSource> context) {
            int delayed = IntegerArgumentType.getInteger(context, "delayed");
            return switch (this) {
                case TICK -> delayed;
                case SECOND -> delayed * 20L;
                case MINUTE -> delayed * 1200L;
                case HOUR -> delayed * 72000L;
            };
        }
    }
}
