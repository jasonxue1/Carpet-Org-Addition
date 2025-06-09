package org.carpetorgaddition.util.screen;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerFactory;
import org.carpetorgaddition.util.inventory.ContainerComponentInventory;

public class ScreenHandlerFactories {
    public static ScreenHandlerFactory createShulkerScreenHandler(ItemStack shulker) {
        ContainerComponentInventory inventory = new ContainerComponentInventory(shulker);
        return (syncId, playerInventory, player) -> new QuickShulkerScreenHandler(syncId, playerInventory, inventory, shulker);
    }
}
