package org.carpetorgaddition.util;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.inventory.ContainerComponentInventory;
import org.carpetorgaddition.util.screen.QuickShulkerScreenHandler;

public class ScreenUtils {
    public static void openScreenHandler(ServerPlayerEntity player, ScreenHandlerFactory baseFactory, Text name) {
        SimpleNamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(baseFactory, name);
        player.openHandledScreen(factory);
    }

    /**
     * 打开快捷潜影盒屏幕
     */
    public static void openShulkerScreenHandler(ServerPlayerEntity player, ItemStack shulker) {
        if (shulker.isEmpty() || shulker.getCount() != 1) {
            return;
        }
        if (player.currentScreenHandler instanceof QuickShulkerScreenHandler screenHandler && screenHandler.getShulkerBox() == shulker) {
            return;
        }
        ContainerComponentInventory inventory = new ContainerComponentInventory(shulker);
        ScreenHandlerFactory factory = (syncId, playerInventory, __) -> new QuickShulkerScreenHandler(syncId, playerInventory, inventory, shulker);
        openScreenHandler(player, factory, shulker.getName());
    }
}
