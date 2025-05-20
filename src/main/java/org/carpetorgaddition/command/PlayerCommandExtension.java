package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.value.OpenPlayerInventory;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.OfflinePlayerEnderChestInventory;
import org.carpetorgaddition.util.inventory.OfflinePlayerInventory;
import org.carpetorgaddition.util.screen.OfflinePlayerInventoryScreenHandler;
import org.carpetorgaddition.util.screen.PlayerEnderChestScreenHandler;
import org.carpetorgaddition.util.screen.PlayerInventoryScreenHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerCommandExtension {
    public static RequiredArgumentBuilder<ServerCommandSource, ?> register(RequiredArgumentBuilder<ServerCommandSource, ?> builder) {
        return builder
                .then(CommandManager.literal("inventory")
                        .requires(OpenPlayerInventory::isEnable)
                        .executes(PlayerCommandExtension::openPlayerInventory))
                .then(CommandManager.literal("enderChest")
                        .requires(OpenPlayerInventory::isEnable)
                        .executes(PlayerCommandExtension::openEnderChest))
                .then(CommandManager.literal("teleport")
                        .requires(source -> CarpetOrgAdditionSettings.playerCommandTeleportFakePlayer)
                        .executes(PlayerCommandExtension::fakePlayerTeleport));
    }

    // 打开玩家物品栏
    private static int openPlayerInventory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        String playerName = getPlayerName(context);
        ServerPlayerEntity argumentPlayer = getPlayerNullable(playerName, server);
        OpenPlayerInventory ruleValue = CarpetOrgAdditionSettings.playerCommandOpenPlayerInventory;
        switch (argumentPlayer) {
            case null -> {
                if (ruleValue.canOpenOfflinePlayer()) {
                    openOfflinePlayerInventory(playerName, server, sourcePlayer, source);
                } else {
                    throw CommandUtils.createPlayerNotFoundException();
                }
            }
            case EntityPlayerMPFake player -> {
                if (ruleValue.canOpenFakePlayer()) {
                    openOnlinePlayerInventory(sourcePlayer, player, server, source);
                }
            }
            case ServerPlayerEntity player -> {
                if (ruleValue.canOpenRealPlayer()) {
                    openOnlinePlayerInventory(sourcePlayer, player, server, source);
                } else {
                    throw CommandUtils.createNotFakePlayerException(player);
                }
            }
        }
        return 1;
    }

    private static void openOfflinePlayerInventory(
            String username,
            MinecraftServer server,
            ServerPlayerEntity sourcePlayer,
            ServerCommandSource source
    ) throws CommandSyntaxException {
        Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(username, server);
        if (optional.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.player.inventory.offline.no_file_found");
        }
        GameProfile gameProfile = optional.get();
        OfflinePlayerInventory.checkPermission(server, gameProfile, source);
        SimpleNamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) -> {
                    OfflinePlayerInventory inventory = new OfflinePlayerInventory(server, gameProfile);
                    return new OfflinePlayerInventoryScreenHandler(syncId, playerInventory, inventory);
                }, offlinePlayerName(username));
        sourcePlayer.openHandledScreen(factory);
    }

    private static void openOnlinePlayerInventory(
            ServerPlayerEntity sourcePlayer,
            ServerPlayerEntity argumentPlayer,
            MinecraftServer server,
            ServerCommandSource source
    ) throws CommandSyntaxException {
        OfflinePlayerInventory.checkPermission(server, argumentPlayer.getGameProfile(), source);
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new PlayerInventoryScreenHandler(syncId, inventory, argumentPlayer),
                argumentPlayer.getName()
        );
        // 打开物品栏
        sourcePlayer.openHandledScreen(screen);
    }

    // 打开玩家末影箱
    private static int openEnderChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        String playerName = getPlayerName(context);
        ServerPlayerEntity argumentPlayer = getPlayerNullable(playerName, server);
        OpenPlayerInventory ruleValue = CarpetOrgAdditionSettings.playerCommandOpenPlayerInventory;
        switch (argumentPlayer) {
            case null -> {
                if (ruleValue.canOpenOfflinePlayer()) {
                    openOfflinePlayerEnderChest(playerName, server, sourcePlayer, source);
                } else {
                    throw CommandUtils.createPlayerNotFoundException();
                }
            }
            case EntityPlayerMPFake player -> {
                if (ruleValue.canOpenFakePlayer()) {
                    openOnlinePlayerEnderChest(sourcePlayer, player, server, source);
                }
            }
            case ServerPlayerEntity player -> {
                if (ruleValue.canOpenRealPlayer()) {
                    openOnlinePlayerEnderChest(sourcePlayer, player, server, source);
                } else {
                    throw CommandUtils.createNotFakePlayerException(player);
                }
            }
        }
        return 1;
    }

    private static void openOfflinePlayerEnderChest(
            String username,
            MinecraftServer server,
            ServerPlayerEntity sourcePlayer,
            ServerCommandSource source
    ) throws CommandSyntaxException {
        Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(username, server);
        if (optional.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.player.inventory.offline.no_file_found");
        }
        GameProfile gameProfile = optional.get();
        OfflinePlayerInventory.checkPermission(server, gameProfile, source);
        SimpleNamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) -> {
                    OfflinePlayerEnderChestInventory inventory = new OfflinePlayerEnderChestInventory(server, gameProfile);
                    return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory);
                }, offlinePlayerName(username));
        sourcePlayer.openHandledScreen(factory);
    }

    private static void openOnlinePlayerEnderChest(
            ServerPlayerEntity sourcePlayer,
            ServerPlayerEntity argumentPlayer,
            MinecraftServer server,
            ServerCommandSource source
    ) throws CommandSyntaxException {
        OfflinePlayerInventory.checkPermission(server, argumentPlayer.getGameProfile(), source);
        // 创建GUI对象
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory(
                (i, inventory, player) -> new PlayerEnderChestScreenHandler(i, inventory, argumentPlayer),
                argumentPlayer.getName()
        );
        // 打开末影箱GUI
        sourcePlayer.openHandledScreen(screen);
    }

    // 传送假玩家
    private static int fakePlayerTeleport(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity fakePlayer = getPlayer(context);
        // 断言指定玩家为假玩家
        CommandUtils.assertFakePlayer(fakePlayer);
        // 在假玩家位置播放潜影贝传送音效
        fakePlayer.getWorld().playSound(null, fakePlayer.prevX, fakePlayer.prevY, fakePlayer.prevZ,
                SoundEvents.ENTITY_SHULKER_TELEPORT, fakePlayer.getSoundCategory(), 1.0f, 1.0f);
        // 传送玩家
        fakePlayer.teleport(player.getServerWorld(), player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        // 获取假玩家名和命令执行玩家名
        Text fakePlayerName = fakePlayer.getDisplayName();
        Text playerName = player.getDisplayName();
        // 在聊天栏显示命令反馈
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.player.tp.success", fakePlayerName, playerName);
        return 1;
    }

    private static Text offlinePlayerName(String name) {
        return TextUtils.translate("carpet.commands.player.inventory.offline.display_name", name);
    }

    @NotNull
    private static ServerPlayerEntity getPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = getPlayerNullable(getPlayerName(context), context.getSource().getServer());
        if (player == null) {
            throw CommandUtils.createPlayerNotFoundException();
        }
        return player;
    }

    @Nullable
    private static ServerPlayerEntity getPlayerNullable(String name, MinecraftServer server) {
        return server.getPlayerManager().getPlayer(name);
    }

    private static String getPlayerName(CommandContext<ServerCommandSource> context) {
        return StringArgumentType.getString(context, "player");
    }
}
