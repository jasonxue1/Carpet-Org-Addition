package org.carpetorgaddition.util.inventory;

import com.mojang.authlib.GameProfile;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;

public class OfflinePlayerEnderChestInventory extends OfflinePlayerInventory {
    public OfflinePlayerEnderChestInventory(MinecraftServer server, GameProfile gameProfile) {
        super(server, gameProfile);
    }

    @Override
    public int size() {
        return this.fabricPlayer.getEnderChestInventory().size();
    }

    @Override
    protected Inventory getInventory() {
        return this.fabricPlayer.getEnderChestInventory();
    }
}
