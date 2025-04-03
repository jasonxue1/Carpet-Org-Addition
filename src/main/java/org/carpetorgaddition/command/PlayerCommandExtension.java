package org.carpetorgaddition.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
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
import org.carpetorgaddition.util.screen.PlayerEnderChestScreenHandler;
import org.carpetorgaddition.util.screen.PlayerInventoryScreenHandler;
import org.jetbrains.annotations.NotNull;

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
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity argumentPlayer = getPlayer(context);
        switch (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventory) {
            case FALSE:
                throw new IllegalStateException();
            case FAKE_PLAYER:
                CommandUtils.assertFakePlayer(argumentPlayer);
            case ONLINE_PLAYER: {
                SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory(
                        (syncId, inventory, player) -> new PlayerInventoryScreenHandler(syncId, inventory, argumentPlayer),
                        argumentPlayer.getName()
                );
                // 打开物品栏
                sourcePlayer.openHandledScreen(screen);
            }
            default: {
            }
        }
        return 1;
    }

    // 打开玩家末影箱
    private static int openEnderChest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        PlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity argumentPlayer = getPlayer(context);
        switch (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventory) {
            case FALSE:
                throw new IllegalStateException();
            case FAKE_PLAYER:
                CommandUtils.assertFakePlayer(argumentPlayer);
            case ONLINE_PLAYER: {
                // 创建GUI对象
                SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory(
                        (i, inventory, playerEntity1) -> new PlayerEnderChestScreenHandler(i, inventory, argumentPlayer),
                        argumentPlayer.getName()
                );
                // 打开末影箱GUI
                sourcePlayer.openHandledScreen(screen);
            }
            default: {
            }
        }
        return 1;
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
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.playerTools.tp.success", fakePlayerName, playerName);
        return 1;
    }

    @NotNull
    private static ServerPlayerEntity getPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(context, "player");
        MinecraftServer server = context.getSource().getServer();
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (player == null) {
            throw CommandUtils.createPlayerNotFoundException();
        }
        return player;
    }
}
