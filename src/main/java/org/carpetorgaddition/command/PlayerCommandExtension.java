package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.inventory.OfflinePlayerEnderChestInventory;
import org.carpetorgaddition.wheel.inventory.OfflinePlayerInventory;
import org.carpetorgaddition.wheel.screen.OfflinePlayerInventoryScreenHandler;
import org.carpetorgaddition.wheel.screen.PlayerEnderChestScreenHandler;
import org.carpetorgaddition.wheel.screen.PlayerInventoryScreenHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PlayerCommandExtension {
    public static RequiredArgumentBuilder<ServerCommandSource, ?> register(RequiredArgumentBuilder<ServerCommandSource, ?> builder) {
        return builder
                .then(CommandManager.literal("inventory")
                        .requires(OpenPlayerInventory::isEnable)
                        .executes(context -> openPlayerInventory(context, false))
                        .then(CommandManager.argument("caseSensitive", BoolArgumentType.bool())
                                .executes(context -> openPlayerInventory(context, BoolArgumentType.getBool(context, "caseSensitive")))))
                .then(CommandManager.literal("enderChest")
                        .requires(OpenPlayerInventory::isEnable)
                        .executes(context -> openEnderChest(context, false))
                        .then(CommandManager.argument("caseSensitive", BoolArgumentType.bool())
                                .executes(context -> openEnderChest(context, BoolArgumentType.getBool(context, "caseSensitive")))))
                .then(CommandManager.literal("teleport")
                        .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.playerCommandTeleportFakePlayer))
                        .executes(PlayerCommandExtension::fakePlayerTeleport));
    }

    // 打开玩家物品栏
    private static int openPlayerInventory(CommandContext<ServerCommandSource> context, boolean caseSensitive) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String playerName = getPlayerName(context);
        MinecraftServer server = source.getServer();
        ServerPlayerEntity argumentPlayer = getPlayerNullable(playerName, server);
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(source);
        OpenPlayerInventory ruleValue = CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get();
        switch (argumentPlayer) {
            case null -> {
                Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(playerName, caseSensitive, server);
                if (optional.isEmpty()) {
                    throw PlayerCommandExtension.createNoFileFoundException();
                }
                GameProfile gameProfile = optional.get();
                openOfflinePlayerInventory(sourcePlayer, gameProfile);
            }
            case EntityPlayerMPFake player -> {
                if (ruleValue.canOpenFakePlayer()) {
                    openOnlinePlayerInventory(sourcePlayer, player);
                }
            }
            case ServerPlayerEntity player -> {
                if (ruleValue.canOpenRealPlayer()) {
                    openOnlinePlayerInventory(sourcePlayer, player);
                } else {
                    throw CommandUtils.createNotFakePlayerException(player);
                }
            }
        }
        return 1;
    }

    public static void openOfflinePlayerInventory(ServerPlayerEntity sourcePlayer, GameProfile gameProfile) throws CommandSyntaxException {
        MinecraftServer server = GenericUtils.getServer(sourcePlayer);
        if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get().canOpenOfflinePlayer()) {
            if (gameProfile == null) {
                throw createNoFileFoundException();
            }
            OfflinePlayerInventory.checkPermission(server, gameProfile, sourcePlayer);
            SimpleNamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, player) -> {
                        OfflinePlayerInventory inventory = new OfflinePlayerInventory(server, gameProfile);
                        return new OfflinePlayerInventoryScreenHandler(syncId, playerInventory, inventory);
                    }, offlinePlayerName(gameProfile.name()));
            sourcePlayer.openHandledScreen(factory);
        } else {
            throw CommandUtils.createPlayerNotFoundException();
        }
    }

    public static CommandSyntaxException createNoFileFoundException() {
        return CommandUtils.createException("carpet.commands.player.inventory.offline.no_file_found");
    }

    public static void openOnlinePlayerInventory(ServerPlayerEntity sourcePlayer, ServerPlayerEntity argumentPlayer) throws CommandSyntaxException {
        MinecraftServer server = GenericUtils.getServer(sourcePlayer);
        OfflinePlayerInventory.checkPermission(server, argumentPlayer.getGameProfile(), sourcePlayer);
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new PlayerInventoryScreenHandler(syncId, inventory, argumentPlayer),
                argumentPlayer.getName()
        );
        // 打开物品栏
        sourcePlayer.openHandledScreen(screen);
    }

    // 打开玩家末影箱
    private static int openEnderChest(CommandContext<ServerCommandSource> context, boolean caseSensitive) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        String playerName = getPlayerName(context);
        ServerPlayerEntity argumentPlayer = getPlayerNullable(playerName, server);
        OpenPlayerInventory ruleValue = CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get();
        switch (argumentPlayer) {
            case null -> {
                Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(playerName, caseSensitive, server);
                if (optional.isEmpty()) {
                    throw createNoFileFoundException();
                }
                GameProfile gameProfile = optional.get();
                openOfflinePlayerEnderChest(sourcePlayer, gameProfile);
            }
            case EntityPlayerMPFake player -> {
                if (ruleValue.canOpenFakePlayer()) {
                    openOnlinePlayerEnderChest(sourcePlayer, player);
                }
            }
            case ServerPlayerEntity player -> {
                if (ruleValue.canOpenRealPlayer()) {
                    openOnlinePlayerEnderChest(sourcePlayer, player);
                } else {
                    throw CommandUtils.createNotFakePlayerException(player);
                }
            }
        }
        return 1;
    }

    public static void openOfflinePlayerEnderChest(ServerPlayerEntity sourcePlayer, GameProfile gameProfile) throws CommandSyntaxException {
        MinecraftServer server = GenericUtils.getServer(sourcePlayer);
        if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get().canOpenOfflinePlayer()) {
            OfflinePlayerInventory.checkPermission(server, gameProfile, sourcePlayer);
            SimpleNamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, player) -> {
                        OfflinePlayerEnderChestInventory inventory = new OfflinePlayerEnderChestInventory(server, gameProfile);
                        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, inventory);
                    }, offlinePlayerName(gameProfile.name()));
            sourcePlayer.openHandledScreen(factory);
        } else {
            throw CommandUtils.createPlayerNotFoundException();
        }
    }

    public static void openOnlinePlayerEnderChest(ServerPlayerEntity sourcePlayer, ServerPlayerEntity argumentPlayer) throws CommandSyntaxException {
        MinecraftServer server = GenericUtils.getServer(sourcePlayer);
        OfflinePlayerInventory.checkPermission(server, argumentPlayer.getGameProfile(), sourcePlayer);
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
        fakePlayer.getEntityWorld().playSound(null, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.ENTITY_SHULKER_TELEPORT, fakePlayer.getSoundCategory(), 1.0f, 1.0f);
        // 传送玩家
        WorldUtils.teleport(fakePlayer, player);
        // 获取假玩家名和命令执行玩家名
        Text fakePlayerName = fakePlayer.getDisplayName();
        Text playerName = player.getDisplayName();
        // 在聊天栏显示命令反馈
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.player.tp.success", fakePlayerName, playerName);
        return 1;
    }

    private static Text offlinePlayerName(String name) {
        return TextBuilder.translate("carpet.commands.player.inventory.offline.display_name", name);
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
