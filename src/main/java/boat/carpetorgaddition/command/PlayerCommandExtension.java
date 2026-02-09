package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.rule.value.OpenPlayerInventory;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.inventory.*;
import boat.carpetorgaddition.wheel.screen.OfflinePlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.screen.PlayerEnderChestScreenHandler;
import boat.carpetorgaddition.wheel.screen.PlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.screen.WithButtonPlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PlayerCommandExtension {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("player");
    public static final LocalizationKey INVENTORY = KEY.then("inventory");

    // TODO 添加召唤玩家模型子命令
    public static RequiredArgumentBuilder<CommandSourceStack, ?> register(RequiredArgumentBuilder<CommandSourceStack, ?> builder) {
        return builder
                .then(Commands.literal("inventory")
                        .requires(OpenPlayerInventory::isEnable)
                        .executes(context -> openInventory(context, PlayerInventoryType.INVENTORY)))
                .then(Commands.literal("enderChest")
                        .requires(OpenPlayerInventory::isEnable)
                        .executes(context -> openInventory(context, PlayerInventoryType.ENDER_CHEST)))
                .then(Commands.literal("teleport")
                        .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.playerCommandTeleportFakePlayer))
                        .executes(PlayerCommandExtension::fakePlayerTeleport));
    }

    private static int openInventory(CommandContext<CommandSourceStack> context, PlayerInventoryType type) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer visitor = CommandUtils.getSourcePlayer(source);
        String name = getPlayerName(context);
        MinecraftServer server = source.getServer();
        ServerPlayer interviewee = getPlayerNullable(name, server);
        PlayerInventroyAccessor accessor = (interviewee == null ? new PlayerInventroyAccessor(server, name, visitor) : new PlayerInventroyAccessor(interviewee, visitor));
        return openInventory(visitor, type, accessor);
    }

    @NullMarked
    public static int openInventory(ServerPlayer player, PlayerInventoryType type, PlayerInventroyAccessor accessor) throws CommandSyntaxException {
        CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.value().checkPermission(player, accessor.getGameProfile());
        PlayerUtils.openScreenHandler(
                player,
                (containerId, inventory, serverPlayer) -> accessor.createMenu(containerId, inventory, serverPlayer, type),
                accessor.getDisplayName()
        );
        return 1;
    }

    public static CommandSyntaxException createNoFileFoundException() {
        return CommandUtils.createException(INVENTORY.then("no_file_found").translate());
    }

    // 传送假玩家
    private static int fakePlayerTeleport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        ServerPlayer fakePlayer = getPlayer(context);
        // 断言指定玩家为假玩家
        CommandUtils.requireFakePlayer(fakePlayer);
        // 在假玩家位置播放潜影贝传送音效
        ServerUtils.getWorld(fakePlayer).playSound(null, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
                SoundEvents.SHULKER_TELEPORT, fakePlayer.getSoundSource(), 1.0f, 1.0f);
        // 传送玩家
        ServerUtils.teleport(fakePlayer, player);
        // 获取假玩家名和命令执行玩家名
        Component fakePlayerName = fakePlayer.getDisplayName();
        Component playerName = player.getDisplayName();
        // 在聊天栏显示命令反馈
        LocalizationKey key = LocalizationKey.literal("commands.teleport.success.entity.single");
        MessageUtils.sendMessage(context.getSource(), key.translate(fakePlayerName, playerName));
        return 1;
    }

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

    @NullMarked
    public static class PlayerInventroyAccessor {
        private final MenuConstructor inventory;
        private final MenuConstructor enderChest;
        private final Component displayName;
        private final GameProfile gameProfile;

        public PlayerInventroyAccessor(ServerPlayer interviewee, ServerPlayer visitor) throws CommandSyntaxException {
            checkCanBeOpened(interviewee);
            this.displayName = new TextJoiner()
                    .append(TextBuilder.ofPlayerAvatar(interviewee).setColor(ChatFormatting.WHITE).build())
                    .append(" ")
                    .append(interviewee.getDisplayName())
                    .join();
            this.gameProfile = interviewee.getGameProfile();
            this.inventory = (containerId, inventory, _) -> {
                if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryGcaStyle.value()) {
                    return new WithButtonPlayerInventoryScreenHandler(containerId, interviewee, visitor);
                } else {
                    return new PlayerInventoryScreenHandler(containerId, inventory, interviewee);
                }
            };
            this.enderChest = (containerId, inventory, _) -> new PlayerEnderChestScreenHandler(containerId, inventory, interviewee);
        }

        public PlayerInventroyAccessor(MinecraftServer server, GameProfile gameProfile, ServerPlayer visitor) throws CommandSyntaxException {
            this.gameProfile = gameProfile;
            ServerPlayer interviewee = server.getPlayerList().getPlayer(gameProfile.id());
            checkCanBeOpened(interviewee);
            if (interviewee == null) {
                FabricPlayerAccessManager accessManager = ServerComponentCoordinator.getCoordinator(server).getAccessManager();
                FabricPlayerAccessor accessor = accessManager.getOrCreate(gameProfile);
                Component name = LocalizationKeys.Operation.OFFLINE_PLAYER_NAME.translate(gameProfile.name());
                this.displayName = new TextJoiner()
                        .append(TextBuilder.ofPlayerAvatar(gameProfile.id()).setColor(ChatFormatting.WHITE).build())
                        .append(" ")
                        .append(name)
                        .join();
                this.inventory = (containerId, inventory, _) -> new OfflinePlayerInventoryScreenHandler(containerId, inventory, new OfflinePlayerInventory(accessor));
                this.enderChest = (containerId, inventory, _) -> ChestMenu.threeRows(containerId, inventory, new OfflinePlayerEnderChestInventory(accessor));
            } else {
                this.displayName = new TextJoiner()
                        .append(TextBuilder.ofPlayerAvatar(interviewee).setColor(ChatFormatting.WHITE).build())
                        .append(" ")
                        .append(interviewee.getDisplayName())
                        .join();
                this.inventory = (containerId, inventory, _) -> {
                    if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryGcaStyle.value()) {
                        return new WithButtonPlayerInventoryScreenHandler(containerId, interviewee, visitor);
                    } else {
                        return new PlayerInventoryScreenHandler(containerId, inventory, interviewee);
                    }
                };
                this.enderChest = (containerId, inventory, _) -> new PlayerEnderChestScreenHandler(containerId, inventory, interviewee);
            }
        }

        public PlayerInventroyAccessor(MinecraftServer server, String name, ServerPlayer visitor) throws CommandSyntaxException {
            checkCanBeOpened(server.getPlayerList().getPlayer(name));
            Optional<GameProfile> optional = OfflinePlayerInventory.getGameProfile(name, server);
            if (optional.isEmpty()) {
                throw createNoFileFoundException();
            }
            this(server, optional.get(), visitor);
        }

        public PlayerInventroyAccessor(MinecraftServer server, UUID uuid, ServerPlayer visitor) throws CommandSyntaxException {
            checkCanBeOpened(server.getPlayerList().getPlayer(uuid));
            Optional<GameProfile> optional = OfflinePlayerInventory.getPlayerConfigEntry(uuid, server).map(entry -> new GameProfile(entry.id(), entry.name()));
            if (optional.isEmpty()) {
                throw createNoFileFoundException();
            }
            this(server, optional.get(), visitor);
        }

        public Component getDisplayName() {
            return this.displayName;
        }

        public GameProfile getGameProfile() {
            return this.gameProfile;
        }

        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player, PlayerInventoryType type) {
            AbstractContainerMenu menu = switch (type) {
                case INVENTORY -> this.inventory.createMenu(containerId, inventory, player);
                case ENDER_CHEST -> this.enderChest.createMenu(containerId, inventory, player);
            };
            if (menu == null) {
                throw new IllegalStateException("Failed to create container menu");
            }
            return menu;
        }

        private static void checkCanBeOpened(@Nullable ServerPlayer player) throws CommandSyntaxException {
            OpenPlayerInventory option = CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.value();
            switch (player) {
                case EntityPlayerMPFake _ -> {
                    if (option.canOpenFakePlayer()) {
                        return;
                    }
                    throw new IllegalStateException("Always allow opening fake player inventory");
                }
                case ServerPlayer _ -> {
                    if (option.canOpenRealPlayer()) {
                        return;
                    }
                    throw CommandUtils.createNotFakePlayerException(player);
                }
                case null -> {
                    if (option.canOpenOfflinePlayer()) {
                        return;
                    }
                    throw CommandUtils.createPlayerNotFoundException();
                }
            }
        }
    }
}
