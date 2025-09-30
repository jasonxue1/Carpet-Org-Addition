package org.carpetorgaddition.wheel.inventory;

import net.minecraft.inventory.Inventory;

public class OfflinePlayerEnderChestInventory extends OfflinePlayerInventory {
    public OfflinePlayerEnderChestInventory(FabricPlayerAccessor accessor) {
        super(accessor);
    }

    @Override
    public int size() {
        return this.accessor.getEnderChest().size();
    }

    @Override
    protected Inventory getInventory() {
        return this.accessor.getEnderChest();
    }
}
