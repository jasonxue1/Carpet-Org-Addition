package org.carpetorgaddition.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;
import org.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import org.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;

import java.util.function.Predicate;

public class ScreenUtils {
    public static void openScreenHandler(ServerPlayer player, MenuConstructor baseFactory, Component name) {
        SimpleMenuProvider factory = new SimpleMenuProvider(baseFactory, name);
        player.openMenu(factory);
    }

    /**
     * 打开快捷潜影盒屏幕
     */
    public static void openShulkerScreenHandler(ServerPlayer player, ItemStack shulker) {
        if (shulker.isEmpty() || shulker.getCount() != 1) {
            return;
        }
        AbstractContainerMenu currentScreenHandler = player.containerMenu;
        if (currentScreenHandler instanceof QuickShulkerScreenHandler screenHandler && screenHandler.getShulkerBox() == shulker) {
            return;
        }
        // 玩家可能在箱子中打开快捷潜影盒，如果这时玩家离开箱子过远导致箱子所在区块被卸载，则可能导致物品复制，因此就需要在离开箱子时关闭潜影盒
        Predicate<Player> predicate = currentScreenHandler::stillValid;
        if (predicate.negate().test(player)) {
            return;
        }
        ContainerComponentInventory inventory = new ContainerComponentInventory(shulker);
        MenuConstructor factory = (syncId, playerInventory, __) ->
                new QuickShulkerScreenHandler(syncId, playerInventory, inventory, player, predicate, shulker);
        openScreenHandler(player, factory, shulker.getHoverName());
    }
}
