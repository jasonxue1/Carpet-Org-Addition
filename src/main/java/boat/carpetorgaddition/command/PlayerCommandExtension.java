package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.rule.value.OpenPlayerInventory;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.WorldUtils;
import boat.carpetorgaddition.wheel.TextBuilder;
import boat.carpetorgaddition.wheel.inventory.*;
import boat.carpetorgaddition.wheel.screen.OfflinePlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.screen.PlayerEnderChestScreenHandler;
import boat.carpetorgaddition.wheel.screen.PlayerInventoryScreenHandler;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PlayerCommandExtension {
    public static RequiredArgumentBuilder<CommandSourceStack, ?> register(RequiredArgumentBuilder<CommandSourceStack, ?> builder) {
        return builder
                .then(Commands.literal("inventory")
                        .requires(OpenPlayerInventory::isEnable)
                        .executes(PlayerCommandExtension::openPlayerInventory))
                .then(Commands.literal("enderChest")
                        .requires(OpenPlayerInventory::isEnable)
                        .executes(PlayerCommandExtension::openEnderChest))
                .then(Commands.literal("teleport")
                        .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.playerCommandTeleportFakePlayer))
                        .executes(PlayerCommandExtension::fakePlayerTeleport));
    }

    // 打开玩家物品栏
    private static int openPlayerInventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String playerName = getPlayerName(context);
        MinecraftServer server = source.getServer();
        ServerPlayer argumentPlayer = getPlayerNullable(playerName, server);
        ServerPlayer sourcePlayer = CommandUtils.getSourcePlayer(source);
        OpenPlayerInventory ruleValue = CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get();
        switch (argumentPlayer) {
            case null -> {
                if (ruleValue.canOpenOfflinePlayer()) {
                    Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(playerName, server);
                    if (optional.isEmpty()) {
                        throw PlayerCommandExtension.createNoFileFoundException();
                    }
                    GameProfile gameProfile = optional.get();
                    openOfflinePlayerInventory(sourcePlayer, gameProfile);
                } else {
                    throw CommandUtils.createPlayerNotFoundException();
                }
            }
            case EntityPlayerMPFake player -> {
                if (ruleValue.canOpenFakePlayer()) {
                    openOnlinePlayerInventory(sourcePlayer, player);
                }
            }
            case ServerPlayer player -> {
                if (ruleValue.canOpenRealPlayer()) {
                    openOnlinePlayerInventory(sourcePlayer, player);
                } else {
                    throw CommandUtils.createNotFakePlayerException(player);
                }
            }
        }
        return 1;
    }

    public static void openOfflinePlayerInventory(ServerPlayer sourcePlayer, GameProfile gameProfile) throws CommandSyntaxException {
        MinecraftServer server = FetcherUtils.getServer(sourcePlayer);
        if (gameProfile == null) {
            throw createNoFileFoundException();
        }
        OfflinePlayerInventory.checkPermission(server, gameProfile, sourcePlayer);
        SimpleMenuProvider factory = new SimpleMenuProvider(
                (syncId, playerInventory, _) -> {
                    FabricPlayerAccessManager accessManager = ServerComponentCoordinator.getCoordinator(server).getAccessManager();
                    FabricPlayerAccessor accessor = accessManager.getOrCreate(gameProfile);
                    OfflinePlayerInventory inventory = new OfflinePlayerInventory(accessor);
                    return new OfflinePlayerInventoryScreenHandler(syncId, playerInventory, inventory);
                }, offlinePlayerName(gameProfile.name()));
        sourcePlayer.openMenu(factory);
    }

    public static CommandSyntaxException createNoFileFoundException() {
        return CommandUtils.createException("carpet.commands.player.inventory.offline.no_file_found");
    }

    public static void openOnlinePlayerInventory(ServerPlayer sourcePlayer, ServerPlayer argumentPlayer) throws CommandSyntaxException {
        MinecraftServer server = FetcherUtils.getServer(sourcePlayer);
        OfflinePlayerInventory.checkPermission(server, argumentPlayer.getGameProfile(), sourcePlayer);
        SimpleMenuProvider screen = new SimpleMenuProvider(
                (syncId, inventory, _) -> new PlayerInventoryScreenHandler(syncId, inventory, argumentPlayer),
                argumentPlayer.getName()
        );
        // 打开物品栏
        sourcePlayer.openMenu(screen);
    }

    // 打开玩家末影箱
    private static int openEnderChest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayer sourcePlayer = CommandUtils.getSourcePlayer(context);
        String playerName = getPlayerName(context);
        ServerPlayer argumentPlayer = getPlayerNullable(playerName, server);
        OpenPlayerInventory ruleValue = CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get();
        switch (argumentPlayer) {
            case null -> {
                if (ruleValue.canOpenOfflinePlayer()) {
                    Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(playerName, server);
                    if (optional.isEmpty()) {
                        throw createNoFileFoundException();
                    }
                    GameProfile gameProfile = optional.get();
                    openOfflinePlayerEnderChest(sourcePlayer, gameProfile);
                } else {
                    throw CommandUtils.createPlayerNotFoundException();
                }
            }
            case EntityPlayerMPFake player -> {
                if (ruleValue.canOpenFakePlayer()) {
                    openOnlinePlayerEnderChest(sourcePlayer, player);
                }
            }
            case ServerPlayer player -> {
                if (ruleValue.canOpenRealPlayer()) {
                    openOnlinePlayerEnderChest(sourcePlayer, player);
                } else {
                    throw CommandUtils.createNotFakePlayerException(player);
                }
            }
        }
        return 1;
    }

    public static void openOfflinePlayerEnderChest(ServerPlayer sourcePlayer, GameProfile gameProfile) throws CommandSyntaxException {
        MinecraftServer server = FetcherUtils.getServer(sourcePlayer);
        OfflinePlayerInventory.checkPermission(server, gameProfile, sourcePlayer);
        SimpleMenuProvider factory = new SimpleMenuProvider(
                (syncId, playerInventory, _) -> {
                    FabricPlayerAccessManager accessManager = ServerComponentCoordinator.getCoordinator(server).getAccessManager();
                    FabricPlayerAccessor accessor = accessManager.getOrCreate(gameProfile);
                    OfflinePlayerEnderChestInventory inventory = new OfflinePlayerEnderChestInventory(accessor);
                    return ChestMenu.threeRows(syncId, playerInventory, inventory);
                }, offlinePlayerName(gameProfile.name()));
        sourcePlayer.openMenu(factory);
    }

    public static void openOnlinePlayerEnderChest(ServerPlayer sourcePlayer, ServerPlayer argumentPlayer) throws CommandSyntaxException {
        MinecraftServer server = FetcherUtils.getServer(sourcePlayer);
        OfflinePlayerInventory.checkPermission(server, argumentPlayer.getGameProfile(), sourcePlayer);
        // 创建GUI对象
        SimpleMenuProvider screen = new SimpleMenuProvider(
                (i, inventory, _) -> new PlayerEnderChestScreenHandler(i, inventory, argumentPlayer),
                argumentPlayer.getName()
        );
        // 打开末影箱GUI
        sourcePlayer.openMenu(screen);
    }

    // 传送假玩家
    private static int fakePlayerTeleport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        ServerPlayer fakePlayer = getPlayer(context);
        // 断言指定玩家为假玩家
        CommandUtils.assertFakePlayer(fakePlayer);
        // 在假玩家位置播放潜影贝传送音效
        FetcherUtils.getWorld(fakePlayer).playSound(null, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.SHULKER_TELEPORT, fakePlayer.getSoundSource(), 1.0f, 1.0f);
        // 传送玩家
        WorldUtils.teleport(fakePlayer, player);
        // 获取假玩家名和命令执行玩家名
        Component fakePlayerName = fakePlayer.getDisplayName();
        Component playerName = player.getDisplayName();
        // 在聊天栏显示命令反馈
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.player.tp.success", fakePlayerName, playerName);
        return 1;
    }

    private static Component offlinePlayerName(String name) {
        return TextBuilder.translate("carpet.commands.player.inventory.offline.display_name", name);
    }

    @NotNull
    private static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = getPlayerNullable(getPlayerName(context), context.getSource().getServer());
        if (player == null) {
            throw CommandUtils.createPlayerNotFoundException();
        }
        return player;
    }

    @Nullable
    private static ServerPlayer getPlayerNullable(String name, MinecraftServer server) {
        return server.getPlayerList().getPlayerByName(name);
    }

    private static String getPlayerName(CommandContext<CommandSourceStack> context) {
        return StringArgumentType.getString(context, "player");
    }

    public static void openPlayerInventory(MinecraftServer server, UUID uuid, ServerPlayer player, PlayerInventoryType type) throws CommandSyntaxException {
        if (server.getPlayerList().getPlayer(uuid) != null) {
            // TODO 取消此条限制
            throw CommandUtils.createException("carpet.clickevent.open_inventory.fail");
        }
        if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get().canOpenOfflinePlayer()) {
            Optional<GameProfile> optional = OfflinePlayerInventory.getPlayerConfigEntry(uuid, server).map(entry -> new GameProfile(entry.id(), entry.name()));
            if (optional.isEmpty()) {
                throw PlayerCommandExtension.createNoFileFoundException();
            }
            GameProfile gameProfile = optional.get();
            switch (type) {
                case INVENTORY -> PlayerCommandExtension.openOfflinePlayerInventory(player, gameProfile);
                case ENDER_CHEST -> PlayerCommandExtension.openOfflinePlayerEnderChest(player, gameProfile);
            }
        } else {
            throw CommandUtils.createPlayerNotFoundException();
        }
    }
}
