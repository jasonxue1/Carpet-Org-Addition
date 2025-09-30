package org.carpetorgaddition.wheel.inventory;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.mixin.accessor.PlayerManagerAccessor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FabricPlayerAccessor {
    private final FakePlayer fabricPlayer;
    private final MinecraftServer server;
    private final FabricPlayerAccessManager accessManager;
    /**
     * 正在查看物品栏的玩家数
     */
    private final Set<ServerPlayerEntity> viewers = new HashSet<>();
    private final GameProfile gameProfile;

    public FabricPlayerAccessor(MinecraftServer server, GameProfile gameProfile, FabricPlayerAccessManager accessManager) {
        this.server = server;
        this.fabricPlayer = FakePlayer.get(server.getOverworld(), gameProfile);
        this.gameProfile = gameProfile;
        this.initFakePlayer(server);
        this.accessManager = accessManager;
        this.accessManager.addViewers(gameProfile, this.viewers);
    }

    /**
     * @see PlayerManager#onPlayerConnect(ClientConnection, ServerPlayerEntity, ConnectedClientData)
     */
    private void initFakePlayer(MinecraftServer server) {
        ErrorReporter.Logging logging = new ErrorReporter.Logging(CarpetOrgAddition.LOGGER);
        try (logging) {
            Optional<ReadView> optional = server.getPlayerManager().loadPlayerData(this.fabricPlayer, logging);
            RegistryKey<World> registryKey = optional.flatMap(nbt -> nbt.read("Dimension", World.CODEC)).orElse(World.OVERWORLD);
            ServerWorld world = server.getWorld(registryKey);
            if (world != null) {
                // 设置玩家所在维度
                this.fabricPlayer.setServerWorld(world);
            }
        }
    }

    public PlayerInventory getInventory() {
        return this.fabricPlayer.getInventory();
    }

    public EnderChestInventory getEnderChest() {
        return this.fabricPlayer.getEnderChestInventory();
    }

    public void onOpen(ServerPlayerEntity player) {
        this.viewers.add(player);
    }

    public void onClose(ServerPlayerEntity player) {
        this.viewers.remove(player);
        if (this.viewers.isEmpty()) {
            try {
                // 保存玩家数据
                PlayerManager playerManager = this.server.getPlayerManager();
                PlayerManagerAccessor accessor = (PlayerManagerAccessor) playerManager;
                accessor.savePlayerEntityData(this.fabricPlayer);
            } catch (RuntimeException e) {
                CarpetOrgAddition.LOGGER.error("Failed to save player data for {}", this.gameProfile.getName(), e);
            } finally {
                this.accessManager.removeAccessor(this.gameProfile);
                this.accessManager.removeViewer(this.gameProfile);
            }
        }
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }
}
