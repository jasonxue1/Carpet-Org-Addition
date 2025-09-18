package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.PlayerComponentCoordinator;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.Waypoint;
import org.carpetorgaddition.wheel.permission.PermissionLevel;
import org.carpetorgaddition.wheel.permission.PermissionManager;
import org.carpetorgaddition.wheel.provider.TextProvider;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class NavigatorCommand extends AbstractServerCommand {
    /**
     * 开始导航文本
     */
    private static final String START_NAVIGATION = "carpet.commands.navigate.start_navigation";

    public NavigatorCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandNavigate))
                .then(CommandManager.literal("entity")
                        .then(CommandManager.argument("entity", EntityArgumentType.entity())
                                .executes(context -> navigateToEntity(context, false, "entity"))
                                .then(CommandManager.literal("continue")
                                        .executes(context -> navigateToEntity(context, true, "entity")))))
                .then(CommandManager.literal("player")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> navigateToEntity(context, false, "player"))
                                .then(CommandManager.literal("continue")
                                        .executes(context -> navigateToEntity(context, true, "player")))))
                .then(CommandManager.literal("waypoint")
                        .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandLocations))
                        .then(CommandManager.argument("waypoint", StringArgumentType.string())
                                .suggests(LocationsCommand.suggestion())
                                .executes(this::navigateToWaypoint)))
                .then(CommandManager.literal("stop")
                        .executes(this::stopNavigate))
                .then(CommandManager.literal("uuid")
                        .then(CommandManager.argument("uuid", StringArgumentType.string())
                                .executes(this::navigateToEntityForUUID)))
                .then(CommandManager.literal("blockPos")
                        .then(CommandManager.argument("blockPos", BlockPosArgumentType.blockPos())
                                .executes(this::navigateToBlock)))
                .then(CommandManager.literal("spawnpoint")
                        .executes(this::navigateToSpawnPoint))
                .then(CommandManager.literal("death")
                        .requires(PermissionManager.register("navigate.death", PermissionLevel.PASS))
                        .executes(context -> navigateToLastDeathLocation(context, true))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(context -> navigateToLastDeathLocation(context, false)))));
    }

    // 开始导航实体
    private int navigateToEntity(CommandContext<ServerCommandSource> context, boolean isContinue, String arguments) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        Entity entity = EntityArgumentType.getEntity(context, arguments);
        // 如果目标是玩家，广播消息
        TextBuilder builder = TextBuilder.of(START_NAVIGATION, player.getDisplayName(), entity.getDisplayName());
        PlayerComponentCoordinator.getManager(player).getNavigatorManager().setNavigator(entity, isContinue);
        if (shouldBeBroadcast(entity, player)) {
            // 设置为斜体淡灰色
            builder.setGrayItalic();
            MessageUtils.broadcastMessage(context.getSource().getServer(), builder.build());
        } else {
            MessageUtils.sendMessage(context.getSource(), builder.build());
        }
        return 1;
    }

    // 开始导航到路径点
    private int navigateToWaypoint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        String waypointArgument = StringArgumentType.getString(context, "waypoint");
        try {
            Waypoint waypoint = Waypoint.load(server, waypointArgument);
            PlayerComponentCoordinator.getManager(player).getNavigatorManager().setNavigator(waypoint);
            MessageUtils.sendMessage(context, START_NAVIGATION, player.getDisplayName(), "[" + waypointArgument + "]");
        } catch (IOException | NullPointerException e) {
            throw CommandUtils.createException("carpet.commands.locations.list.parse", waypointArgument);
        }
        return 1;
    }

    // 根据UUID获取实体并导航
    private int navigateToEntityForUUID(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        UUID uuid;
        try {
            // 解析UUID
            uuid = UUID.fromString(StringArgumentType.getString(context, "uuid"));
        } catch (IllegalArgumentException e) {
            throw CommandUtils.createException("carpet.commands.navigate.parse_uuid_fail");
        }
        // 从服务器寻找这个UUID的实体
        MinecraftServer server = context.getSource().getServer();
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity == null) {
                continue;
            }
            PlayerComponentCoordinator.getManager(player).getNavigatorManager().setNavigator(entity, false);
            TextBuilder builder = TextBuilder.of(START_NAVIGATION, player.getDisplayName(), entity.getDisplayName());
            if (shouldBeBroadcast(entity, player)) {
                // 将字体设置为灰色斜体
                builder.setGrayItalic();
                MessageUtils.broadcastMessage(context.getSource().getServer(), builder.build());
            } else {
                MessageUtils.sendMessage(context.getSource(), builder.build());
            }
            return 1;
        }
        // 未找到实体
        throw EntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
    }

    // 是否应该广播导航消息
    private boolean shouldBeBroadcast(Entity entity, ServerPlayerEntity player) {
        if (entity == player || entity instanceof EntityPlayerMPFake) {
            return false;
        }
        return entity instanceof ServerPlayerEntity;
    }

    // 停止导航
    private int stopNavigate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        PlayerComponentCoordinator.getManager(player).getNavigatorManager().clearNavigator();
        MessageUtils.sendMessageToHud(player, TextBuilder.translate("carpet.commands.navigate.hud.stop"));
        return 1;
    }

    // 导航到指定坐标
    private int navigateToBlock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos blockPos = BlockPosArgumentType.getBlockPos(context, "blockPos");
        World world = FetcherUtils.getWorld(player);
        // 设置导航器，维度为玩家当前所在维度
        PlayerComponentCoordinator.getManager(player).getNavigatorManager().setNavigator(blockPos, world);
        // 发送命令反馈
        MessageUtils.sendMessage(context, START_NAVIGATION, player.getDisplayName(),
                TextProvider.blockPos(blockPos, WorldUtils.getColor(world)));
        return 1;
    }

    // 导航到重生点
    private int navigateToSpawnPoint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        MutableText spawnPoint = TextBuilder.translate("carpet.commands.navigate.name.spawnpoint");
        try {
            WorldProperties.class_12064 respawnData = Objects.requireNonNull(player.getRespawn()).respawnData();
            BlockPos respawnPos = respawnData.method_74897();
            ServerWorld world = FetcherUtils.getServer(player).getWorld(respawnData.method_74894());
            PlayerComponentCoordinator.getManager(player).getNavigatorManager().setNavigator(respawnPos, world, spawnPoint);
        } catch (NullPointerException e) {
            throw CommandUtils.createException("carpet.commands.navigate.unable_to_find", player.getDisplayName(), spawnPoint);
        }
        MessageUtils.sendMessage(context, START_NAVIGATION, player.getDisplayName(), spawnPoint);
        return 1;
    }

    // 导航到上一次死亡位置
    private int navigateToLastDeathLocation(CommandContext<ServerCommandSource> context, boolean self) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity target = self ? player : CommandUtils.getArgumentPlayer(context);
        Optional<GlobalPos> lastDeathPos = target.getLastDeathPos();
        // 导航器目标的名称
        MutableText death = TextBuilder.translate("carpet.commands.navigate.name.last_death_location");
        // 非空判断
        if (lastDeathPos.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.navigate.unable_to_find", target.getDisplayName(), death);
        }
        MutableText name = self ? death : TextBuilder.translate("carpet.commands.navigate.hud.of", target.getDisplayName(), death);
        // 获取死亡坐标和死亡维度
        GlobalPos globalPos = lastDeathPos.get();
        PlayerComponentCoordinator.getManager(player).getNavigatorManager().setNavigator(globalPos.pos(),
                context.getSource().getServer().getWorld(globalPos.dimension()), name);
        TextBuilder builder = TextBuilder.of(START_NAVIGATION, player.getDisplayName(), name);
        if (self || player == target) {
            MessageUtils.sendMessage(player, builder.build());
        } else {
            builder.setGrayItalic();
            MessageUtils.broadcastMessage(context.getSource().getServer(), builder.build());
        }
        return 1;
    }

    @Override
    public String getDefaultName() {
        return "navigate";
    }
}
