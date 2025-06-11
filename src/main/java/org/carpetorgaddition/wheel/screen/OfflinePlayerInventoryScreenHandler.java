package org.carpetorgaddition.wheel.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.carpetorgaddition.wheel.inventory.OfflinePlayerInventory;

public class OfflinePlayerInventoryScreenHandler extends AbstractPlayerInventoryScreenHandler<OfflinePlayerInventory> {
    public OfflinePlayerInventoryScreenHandler(int syncId, PlayerInventory playerInventory, OfflinePlayerInventory inventory) {
        super(syncId, playerInventory, inventory);
        this.inventory.onOpen(playerInventory.player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
