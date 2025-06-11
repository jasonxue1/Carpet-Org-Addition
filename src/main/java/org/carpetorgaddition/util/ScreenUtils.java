package org.carpetorgaddition.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import org.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;

import java.util.function.Predicate;

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
        ScreenHandler currentScreenHandler = player.currentScreenHandler;
        if (currentScreenHandler instanceof QuickShulkerScreenHandler screenHandler && screenHandler.getShulkerBox() == shulker) {
            return;
        }
        // 玩家可能在箱子中打开快捷潜影盒，如果这时玩家离开箱子过远导致箱子所在区块被卸载，则可能导致物品复制，因此就需要在离开箱子时关闭潜影盒
        Predicate<PlayerEntity> predicate = currentScreenHandler == null ? __ -> true : currentScreenHandler::canUse;
        if (predicate.negate().test(player)) {
            return;
        }
        ContainerComponentInventory inventory = new ContainerComponentInventory(shulker);
        ScreenHandlerFactory factory = (syncId, playerInventory, __) ->
                new QuickShulkerScreenHandler(syncId, playerInventory, inventory, player, predicate, shulker);
        openScreenHandler(player, factory, shulker.getName());
    }
}
