package org.carpetorgaddition.command;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.CommandExecuteIOException;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSafeAfkInterface;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerStartupAction;
import org.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import org.carpetorgaddition.periodic.task.ServerTaskManager;
import org.carpetorgaddition.periodic.task.SilentLogoutTask;
import org.carpetorgaddition.periodic.task.batch.BatchKillFakePlayer;
import org.carpetorgaddition.periodic.task.batch.BatchSpawnFakePlayerTask;
import org.carpetorgaddition.periodic.task.schedule.DelayedLoginTask;
import org.carpetorgaddition.periodic.task.schedule.DelayedLogoutTask;
import org.carpetorgaddition.periodic.task.schedule.PlayerScheduleTask;
import org.carpetorgaddition.periodic.task.schedule.ReLoginTask;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.WorldFormat;
import org.carpetorgaddition.wheel.page.PageManager;
import org.carpetorgaddition.wheel.page.PagedCollection;
import org.carpetorgaddition.wheel.permission.PermissionLevel;
import org.carpetorgaddition.wheel.permission.PermissionManager;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class PlayerManagerCommand extends AbstractServerCommand {
    private static final String SAFEAFK_PROPERTIES = "safeafk.properties";

    public PlayerManagerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    // TODO 指定区域，自动摆放加载玩家
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
        // 玩家启动任务节点
        RequiredArgumentBuilder<ServerCommandSource, String> startupNameNode = CommandManager.argument("name", StringArgumentType.string())
                .suggests(defaultSuggests());
        for (FakePlayerStartupAction action : FakePlayerStartupAction.values()) {
            startupNameNode.then(CommandManager.literal(action.toString())
                    .executes(context -> this.addStartupFunction(context, action, 1))
                    .then(CommandManager.argument("delay", TimeArgumentType.time(1))
                            .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"1t", "3t", "5t"}, builder))
                            .executes(context -> this.addStartupFunction(context, action, IntegerArgumentType.getInteger(context, "delay"))))
                    .then(CommandManager.literal("clear")
                            .executes(context -> this.addStartupFunction(context, action, -1))));
        }
        LiteralArgumentBuilder<ServerCommandSource> startupNode = CommandManager.literal("startup");
        startupNode.then(startupNameNode);
        this.dispatcher.register(CommandManager.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandPlayerManager))
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
                                        .executes(this::modifyPlayer)))
                        .then(startupNode))
                .then(CommandManager.literal("group")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(defaultSuggests())
                                        .then(CommandManager.argument("group", StringArgumentType.string())
                                                .suggests(groupSuggests())
                                                .executes(this::addToGroup))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .suggests(defaultSuggests())
                                        .then(CommandManager.argument("group", StringArgumentType.string())
                                                .suggests(groupSuggests())
                                                .executes(this::removeFromGroup))))
                        .then(CommandManager.literal("list")
                                .then(CommandManager.literal("group")
                                        .then(CommandManager.argument("group", StringArgumentType.string())
                                                .suggests(groupSuggests())
                                                .executes(context -> listGroup(context, serializer -> true))
                                                .then(CommandManager.argument("filter", StringArgumentType.string())
                                                        .executes(context -> listGroup(context, serializerPredicate(context))))))
                                .then(CommandManager.literal("ungrouped")
                                        .executes(context -> listUngrouped(context, serializer -> true))
                                        .then(CommandManager.argument("filter", StringArgumentType.string())
                                                .executes(context -> listUngrouped(context, serializerPredicate(context)))))
                                .then(CommandManager.literal("all")
                                        .executes(context -> listAll(context, serializer -> true))
                                        .then(CommandManager.argument("filter", StringArgumentType.string())
                                                .executes(context -> listAll(context, serializerPredicate(context)))))))
                .then(CommandManager.literal("reload")
                        .executes(this::reload))
                .then(CommandManager.literal("autologin")
                        .requires(PermissionManager.register("playerManager.autologin", PermissionLevel.PASS))
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .then(CommandManager.argument("autologin", BoolArgumentType.bool())
                                        .executes(this::setAutoLogin))))
                .then(CommandManager.literal("list")
                        .executes(context -> list(context, null))
                        .then(CommandManager.argument("filter", StringArgumentType.string())
                                .executes(context -> list(context, StringArgumentType.getString(context, "filter")))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("name", StringArgumentType.string())
                                .suggests(defaultSuggests())
                                .executes(this::remove)))
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
                                        .executes(this::querySafeAfk))))
                .then(CommandManager.literal("batch")
                        .then(CommandManager.argument("prefix", StringArgumentType.word())
                                .then(CommandManager.argument("start", IntegerArgumentType.integer(1))
                                        .then(CommandManager.argument("end", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                                .then(CommandManager.literal("spawn")
                                                        .executes(context -> batchSpawn(context, false))
                                                        .then(CommandManager.argument("at", Vec3ArgumentType.vec3())
                                                                .executes(context -> batchSpawn(context, true))))
                                                .then(CommandManager.literal("kill")
                                                        .executes(this::batchKill))
                                                .then(CommandManager.literal("drop")
                                                        .executes(this::batchDrop))
                                                .then(CommandManager.literal("trial")
                                                        .executes(context -> batchTrial(context, false))
                                                        .then(CommandManager.argument("at", Vec3ArgumentType.vec3())
                                                                .executes(context -> batchTrial(context, true)))))))));
    }

    private int listGroup(CommandContext<ServerCommandSource> context, Predicate<FakePlayerSerializer> predicate) throws CommandSyntaxException {
        String group = StringArgumentType.getString(context, "group");
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = FetcherUtils.getFakePlayerSerializationManager(server);
        HashMap<String, HashSet<FakePlayerSerializer>> map = manager.listGroup(predicate);
        HashSet<FakePlayerSerializer> set = map.get(group);
        // 不存在的组
        if (set == null) {
            throw CommandUtils.createException("carpet.commands.playerManager.group.non_existent", group);
        }
        List<Supplier<Text>> list = set.stream().map(FakePlayerSerializer::toTextSupplier).toList();
        PagedCollection collection = FetcherUtils.getPageManager(server).newPagedCollection(context.getSource());
        collection.addContent(list);
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.group.list", group);
        collection.print();
        return collection.length();
    }

    private int listUngrouped(CommandContext<ServerCommandSource> context, Predicate<FakePlayerSerializer> predicate) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = FetcherUtils.getFakePlayerSerializationManager(server);
        HashMap<String, HashSet<FakePlayerSerializer>> map = manager.listGroup(predicate);
        HashSet<FakePlayerSerializer> set = map.get(null);
        if (set == null) {
            Text translate = TextBuilder.translate("carpet.commands.playerManager.group.name.ungrouped");
            throw CommandUtils.createException("carpet.commands.playerManager.group.non_existent", translate);
        }
        List<Supplier<Text>> list = set.stream().map(FakePlayerSerializer::toTextSupplier).toList();
        PagedCollection collection = FetcherUtils.getPageManager(server).newPagedCollection(context.getSource());
        collection.addContent(list);
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.group.list.ungrouped");
        collection.print();
        return collection.length();
    }

    private int listAll(CommandContext<ServerCommandSource> context, Predicate<FakePlayerSerializer> predicate) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = FetcherUtils.getFakePlayerSerializationManager(server);
        List<Supplier<Text>> list = manager.list().stream().filter(predicate).map(FakePlayerSerializer::toTextSupplier).toList();
        PagedCollection collection = FetcherUtils.getPageManager(server).newPagedCollection(context.getSource());
        collection.addContent(list);
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.group.list.all");
        collection.print();
        return collection.length();
    }

    private int addToGroup(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        String group = StringArgumentType.getString(context, "group");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        serializer.addToGroup(group);
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.group.add", serializer.getDisplayName(), group);
        return 1;
    }

    private int removeFromGroup(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        String group = StringArgumentType.getString(context, "group");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        if (serializer.removeFromGroup(group)) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.group.remove", serializer.getDisplayName(), group);
        } else {
            throw CommandUtils.createException("carpet.commands.playerManager.group.remove.fail");
        }
        return 1;
    }

    /**
     * 重新加载玩家数据
     */
    private int reload(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = FetcherUtils.getFakePlayerSerializationManager(server);
        manager.reload();
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.reload");
        return 1;
    }

    private Predicate<FakePlayerSerializer> serializerPredicate(CommandContext<ServerCommandSource> context) {
        return serializer -> serializerPredicate(serializer, StringArgumentType.getString(context, "filter"));
    }

    private Predicate<FakePlayerSerializer> serializerPredicate(@Nullable String filter) {
        if (filter == null) {
            return serializer -> true;
        }
        return serializer -> serializerPredicate(serializer, filter);
    }

    private boolean serializerPredicate(FakePlayerSerializer serializer, String filter) {
        Predicate<String> predicate = s -> s.contains(filter.toLowerCase(Locale.ROOT));
        return predicate.test(serializer.getFakePlayerName().toLowerCase()) || predicate.test(serializer.getComment());
    }

    // cancel子命令自动补全
    @NotNull
    private SuggestionProvider<ServerCommandSource> cancelSuggests() {
        return (context, builder) -> {
            ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(context).getServerTaskManager();
            Stream<String> stream = manager.stream(PlayerScheduleTask.class).map(PlayerScheduleTask::getPlayerName);
            return CommandSource.suggestMatching(stream, builder);
        };
    }

    // 自动补全玩家名
    private SuggestionProvider<ServerCommandSource> defaultSuggests() {
        return (context, builder) -> {
            Stream<String> stream = FetcherUtils.getFakePlayerSerializationManager(context.getSource().getServer())
                    .list()
                    .stream()
                    .map(FakePlayerSerializer::getFakePlayerName)
                    .map(StringArgumentType::escapeIfRequired);
            return CommandSource.suggestMatching(stream, builder);
        };
    }

    private SuggestionProvider<ServerCommandSource> groupSuggests() {
        return (context, builder) -> {
            PlayerSerializationManager manager = FetcherUtils.getFakePlayerSerializationManager(context.getSource().getServer());
            Stream<String> stream = manager.listGroup(serializer -> true)
                    .keySet()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(StringArgumentType::escapeIfRequired);
            return CommandSource.suggestMatching(stream, builder);
        };
    }

    // relogin子命令自动补全
    @NotNull
    private SuggestionProvider<ServerCommandSource> reLoginTaskSuggests() {
        return (context, builder) -> {
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
            // 所有正在周期性上下线的玩家
            List<String> taskList = manager.stream(ReLoginTask.class).map(ReLoginTask::getPlayerName).toList();
            // 所有在线玩家
            List<String> onlineList = server.getPlayerManager()
                    .getPlayerList()
                    .stream()
                    .map(FetcherUtils::getPlayerName)
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
        safeAfk.carpet_Org_Addition$setHealthThreshold(threshold);
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
        List<ServerPlayerEntity> list = context.getSource().getServer()
                .getPlayerManager()
                .getPlayerList()
                .stream()
                .filter(player -> player instanceof EntityPlayerMPFake)
                .toList();
        int count = 0;
        // 遍历所有在线并且设置了安全挂机的假玩家
        for (ServerPlayerEntity player : list) {
            float threshold = ((FakePlayerSafeAfkInterface) player).carpet_Org_Addition$getHealthThreshold();
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
        safeAfk.carpet_Org_Addition$setHealthThreshold(-1);
        if (remove) {
            try {
                saveSafeAfkThreshold(context, -1, fakePlayer);
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
        } else {
            String key = "carpet.commands.playerManager.safeafk.successfully_set_up.cancel";
            Text command = TextProvider.clickRun(CommandProvider.cancelSafeAfkPermanentlyChange(fakePlayer));
            MessageUtils.sendMessage(context, key, fakePlayer.getDisplayName(), command);
        }
        return 1;
    }

    // 查询指定玩家的安全挂机阈值
    private int querySafeAfk(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        float threshold = ((FakePlayerSafeAfkInterface) fakePlayer).carpet_Org_Addition$getHealthThreshold();
        String key = "carpet.commands.playerManager.safeafk.list.each";
        MessageUtils.sendMessage(context, key, fakePlayer.getDisplayName(), threshold);
        return (int) threshold;
    }

    // 保存或删除安全挂机阈值
    private void saveSafeAfkThreshold(CommandContext<ServerCommandSource> context, float threshold, EntityPlayerMPFake fakePlayer) throws IOException {
        String playerName = FetcherUtils.getPlayerName(fakePlayer);
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
            WorldFormat worldFormat = new WorldFormat(FetcherUtils.getServer(player), null);
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
                    String value = properties.getProperty(FetcherUtils.getPlayerName(player));
                    if (value == null) {
                        return;
                    }
                    float threshold = Float.parseFloat(value);
                    safeAfk.carpet_Org_Addition$setHealthThreshold(threshold);
                    // 广播阈值设置的消息
                    String key = "carpet.commands.playerManager.safeafk.successfully_set_up.auto";
                    TextBuilder builder = TextBuilder.of(key, player.getDisplayName(), threshold);
                    builder.setGrayItalic();
                    MessageUtils.broadcastMessage(FetcherUtils.getServer(player), builder.build());
                } catch (NumberFormatException e) {
                    CarpetOrgAddition.LOGGER.error("{}安全挂机阈值设置失败", FetcherUtils.getPlayerName(player), e);
                }
            }
        }
    }

    // 列出每一个玩家
    private int list(CommandContext<ServerCommandSource> context, @Nullable String filter) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = getSerializationManager(server);
        HashMap<String, HashSet<FakePlayerSerializer>> map = manager.listGroup(serializerPredicate(filter));
        if (map.isEmpty()) {
            // 没有玩家被列出
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.list.no_player");
            return 0;
        }
        if (map.size() == 1) {
            // 只有一个组，直接展示玩家
            Map.Entry<String, HashSet<FakePlayerSerializer>> entry = map.entrySet().iterator().next();
            List<Supplier<Text>> list = entry.getValue()
                    .stream()
                    .sorted(Comparator.naturalOrder())
                    .map(FakePlayerSerializer::toTextSupplier)
                    .toList();
            PageManager pageManager = FetcherUtils.getPageManager(server);
            PagedCollection collection = pageManager.newPagedCollection(context.getSource());
            collection.addContent(list);
            MessageUtils.sendEmptyMessage(context);
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.group.list.all");
            collection.print();
            return list.size();
        } else {
            ArrayList<Text> list = new ArrayList<>();
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.list.expand");
            // 未分组，在倒数第二个展示
            TextBuilder ungrouped = null;
            for (Map.Entry<String, HashSet<FakePlayerSerializer>> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    ungrouped = TextBuilder.of("carpet.commands.playerManager.group.name.ungrouped");
                    setStyle(ungrouped, entry.getValue().size(), CommandProvider.listUngroupedPlayer(filter));
                    continue;
                }
                TextBuilder builder = new TextBuilder("[" + entry.getKey() + "]");
                setStyle(builder, entry.getValue().size(), CommandProvider.listGroupPlayer(entry.getKey(), filter));
                list.add(builder.build());
            }
            if (ungrouped != null) {
                list.add(ungrouped.build());
            }
            // 包含所有玩家的组，在最后一个展示
            TextBuilder builder = TextBuilder.of("carpet.commands.playerManager.group.name.all");
            setStyle(builder, manager.size(), CommandProvider.listAllPlayer(filter));
            list.add(builder.build());
            Text message = TextBuilder.joinList(list, TextBuilder.create(" "));
            MessageUtils.sendMessage(context.getSource(), message);
            return list.size();
        }
    }

    private void setStyle(TextBuilder builder, int size, String command) {
        builder.setColor(Formatting.AQUA);
        builder.setHover("carpet.commands.playerManager.group.player", size);
        builder.setCommand(command);
    }

    // 设置注释
    private int setComment(CommandContext<ServerCommandSource> context, boolean remove) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        // 修改注释
        String comment = remove ? null : StringArgumentType.getString(context, "comment");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        serializer.setComment(comment);
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
        String name = StringArgumentType.getString(context, "name");
        boolean autologin = BoolArgumentType.getBool(context, "autologin");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        // 设置自动登录
        serializer.setAutologin(autologin);
        // 发送命令反馈
        if (autologin) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.autologin.setup", serializer.getDisplayName());
        } else {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.autologin.cancel", serializer.getDisplayName());
        }
        return 1;
    }

    // 保存玩家
    private int savePlayerData(CommandContext<ServerCommandSource> context, boolean hasComment) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        PlayerSerializationManager manager = getSerializationManager(server);
        // 玩家数据是否已存在
        String name = FetcherUtils.getPlayerName(fakePlayer);
        if (IOUtils.isValidFileName(name)) {
            throw CommandUtils.createException("carpet.command.file.name.valid");
        }
        Optional<FakePlayerSerializer> optional = manager.get(name);
        if (optional.isPresent()) {
            String command = CommandProvider.playerManagerResave(name);
            // 单击执行命令
            Text clickResave = TextProvider.clickRun(command);
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.save.file_already_exist", clickResave);
            return 0;
        } else {
            String comment = hasComment ? StringArgumentType.getString(context, "comment") : "";
            if (CarpetOrgAdditionSettings.playerManagerForceComment.get() && comment.isBlank()) {
                throw CommandUtils.createException("carpet.rule.message.playerManagerForceComment");
            }
            FakePlayerSerializer serializer = comment.isBlank() ? new FakePlayerSerializer(fakePlayer) : new FakePlayerSerializer(fakePlayer, comment);
            manager.add(serializer);
            // 首次保存
            MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerManager.save.success", fakePlayer.getDisplayName());
            return 1;
        }
    }

    // 修改玩家数据
    private int modifyPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        String name = FetcherUtils.getPlayerName(fakePlayer);
        FakePlayerSerializer oldSerializer = getFakePlayerSerializer(context, name);
        PlayerSerializationManager manager = FetcherUtils.getFakePlayerSerializationManager(context.getSource().getServer());
        FakePlayerSerializer newSerializer = new FakePlayerSerializer(fakePlayer, oldSerializer);
        manager.add(newSerializer);
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerManager.save.resave", fakePlayer.getDisplayName());
        return 1;
    }

    // 生成假玩家
    private int spawnPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        try {
            // 生成假玩家
            FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
            CarpetOrgAdditionSettings.playerSummoner.set(context.getSource().getPlayer());
            serializer.spawn(context.getSource().getServer());
        } catch (RuntimeException e) {
            // 尝试生成假玩家时出现意外问题
            throw CommandUtils.createException(e, "carpet.commands.playerManager.spawn.fail");
        } finally {
            CarpetOrgAdditionSettings.playerSummoner.remove();
        }
        return 1;
    }

    private FakePlayerSerializer getFakePlayerSerializer(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        PlayerSerializationManager manager = getSerializationManager(context);
        Optional<FakePlayerSerializer> optional = manager.get(name);
        if (optional.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.playerManager.cannot_find_file", name);
        }
        return optional.get();
    }

    // 删除玩家信息
    private int remove(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        PlayerSerializationManager manager = getSerializationManager(context);
        Optional<FakePlayerSerializer> optional = manager.get(name);
        if (optional.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.playerManager.delete.non_existent");
        }
        if (manager.remove(optional.get())) {
            // 文件存在且文件删除成功
            MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerManager.delete.success");
            return 1;
        }
        throw CommandUtils.createException("carpet.commands.playerManager.delete.fail");
    }

    private int addStartupFunction(CommandContext<ServerCommandSource> context, FakePlayerStartupAction action, int delay) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
        serializer.addStartupFunction(action, delay);
        return delay;
    }

    // 设置不断重新上线下线
    private int setReLogin(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // 强制启用内存泄漏修复
        if (fixMemoryLeak(context)) {
            // 获取目标假玩家名
            String name = StringArgumentType.getString(context, "name");
            int interval = IntegerArgumentType.getInteger(context, "interval");
            MinecraftServer server = context.getSource().getServer();
            ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(context).getServerTaskManager();
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
                manager.addTask(new ReLoginTask((EntityPlayerMPFake) player, interval, server, context.getSource()));
            } else {
                // 修改周期时间
                optional.get().setInterval(interval);
                MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.relogin.set_interval", name, interval);
            }
            return interval;
        }
        return 0;
    }

    /**
     * 批量生成玩家
     */
    private int batchSpawn(CommandContext<ServerCommandSource> context, boolean at) throws CommandSyntaxException {
        return batchSpawn(context, at, GenericUtils::pass);
    }

    /**
     * 批量生成玩家并踢出玩家
     */
    private int batchTrial(CommandContext<ServerCommandSource> context, boolean at) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        // 异常由服务器命令源进行处理
        ServerCommandSource source = server.getCommandSource();
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        return batchSpawn(context, at, fakePlayer -> CommandUtils.handlingException(() -> taskManager.addTask(new SilentLogoutTask(fakePlayer, 30)), source));
    }

    private static int batchSpawn(CommandContext<ServerCommandSource> context, boolean at, Consumer<EntityPlayerMPFake> consumer) throws CommandSyntaxException {
        int start = IntegerArgumentType.getInteger(context, "start");
        int end = IntegerArgumentType.getInteger(context, "end");
        // 交换最大最小值，玩家可能将end和start参数反向输入
        int temp = Math.min(start, end);
        end = Math.max(start, end);
        start = temp;
        int count = end - start + 1;
        if (count > 256) {
            // 限制单次生成的最大玩家数量
            throw CommandUtils.createException("carpet.commands.playerManager.batch.exceeds_limit", count, 256);
        }
        String prefix = StringArgumentType.getString(context, "prefix");
        // 为假玩家名添加前缀，这不仅仅是为了让名称更统一，也是为了在一定程度上阻止玩家使用其他真玩家的名称召唤假玩家
        prefix = prefix.endsWith("_") ? prefix : prefix + "_";
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        UserCache userCache = server.getUserCache();
        if (userCache == null) {
            CarpetOrgAddition.LOGGER.warn("Server user cache is null");
            return 0;
        }
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        Vec3d vec3d = at ? Vec3ArgumentType.getVec3(context, "at") : player.getPos();
        taskManager.addTask(new BatchSpawnFakePlayerTask(server, userCache, player, prefix, start, end, vec3d, consumer));
        return end - start + 1;
    }

    /**
     * 批量生成玩家
     */
    private int batchKill(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int start = IntegerArgumentType.getInteger(context, "start");
        int end = IntegerArgumentType.getInteger(context, "end");
        int temp = Math.min(start, end);
        end = Math.max(start, end);
        start = temp;
        String prefix = StringArgumentType.getString(context, "prefix");
        prefix = prefix.endsWith("_") ? prefix : prefix + "_";
        MinecraftServer server = context.getSource().getServer();
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        taskManager.addTask(new BatchKillFakePlayer(server, prefix, start, end));
        return end - start + 1;
    }

    /**
     * 批量生成玩家
     */
    private int batchDrop(CommandContext<ServerCommandSource> context) {
        int start = IntegerArgumentType.getInteger(context, "start");
        int end = IntegerArgumentType.getInteger(context, "end");
        int temp = Math.min(start, end);
        end = Math.max(start, end);
        start = temp;
        String prefix = StringArgumentType.getString(context, "prefix");
        prefix = prefix.endsWith("_") ? prefix : prefix + "_";
        MinecraftServer server = context.getSource().getServer();
        int count = 0;
        PlayerManager playerManager = server.getPlayerManager();
        for (int i = start; i <= end; i++) {
            ServerPlayerEntity player = playerManager.getPlayer(prefix + i);
            if (player instanceof EntityPlayerMPFake fakePlayer) {
                EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
                actionPack.drop(-2, true);
                count++;
            }
        }
        return count;
    }

    // 启用内存泄漏修复
    private boolean fixMemoryLeak(CommandContext<ServerCommandSource> context) {
        if (CarpetOrgAdditionSettings.fakePlayerSpawnMemoryLeakFix.get()) {
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
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(context).getServerTaskManager();
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
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(context).getServerTaskManager();
        String name = StringArgumentType.getString(context, "name");
        Optional<DelayedLoginTask> optional = taskManager.stream(DelayedLoginTask.class)
                .filter(task -> Objects.equals(name, task.getPlayerName()))
                .findFirst();
        // 等待时间
        long tick = unit.getDelayed(context);
        Text time = new TextBuilder(TextProvider.tickToTime(tick)).setHover(TextProvider.tickToRealTime(tick)).build();
        if (optional.isEmpty()) {
            // 添加上线任务
            FakePlayerSerializer serializer = getFakePlayerSerializer(context, name);
            taskManager.addTask(new DelayedLoginTask(server, serializer, tick));
            String key = server.getPlayerManager().getPlayer(name) == null
                    // <玩家>将于<时间>后上线
                    ? "carpet.commands.playerManager.schedule.login"
                    // <玩家>将于<时间>后再次尝试上线
                    : "carpet.commands.playerManager.schedule.login.try";
            // 发送命令反馈
            MessageUtils.sendMessage(context, key, serializer.getDisplayName(), time);
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
        Text time = new TextBuilder(TextProvider.tickToTime(tick)).setHover(TextProvider.tickToRealTime(tick)).build();
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
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
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(context).getServerTaskManager();
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
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(context).getServerTaskManager();
        List<PlayerScheduleTask> list = manager.stream(PlayerScheduleTask.class).toList();
        if (list.isEmpty()) {
            MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.list.empty");
        } else {
            list.forEach(task -> task.sendEachMessage(context.getSource()));
        }
        return list.size();
    }

    private PlayerSerializationManager getSerializationManager(MinecraftServer server) {
        return FetcherUtils.getFakePlayerSerializationManager(server);
    }

    private PlayerSerializationManager getSerializationManager(CommandContext<ServerCommandSource> context) {
        return getSerializationManager(context.getSource().getServer());
    }

    @Override
    public String getDefaultName() {
        return "playerManager";
    }

    /**
     * 时间单位
     */
    public enum TimeUnit {
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
