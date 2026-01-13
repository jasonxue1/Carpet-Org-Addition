package boat.carpetorgaddition.util;

import boat.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import boat.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class PlayerUtils {
    private PlayerUtils() {
    }

    /**
     * 打开一个GUI
     */
    public static void openScreenHandler(Player player, MenuConstructor baseFactory, Component name) {
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
        MenuConstructor factory = (syncId, playerInventory, _) ->
                new QuickShulkerScreenHandler(syncId, playerInventory, inventory, player, predicate, shulker);
        openScreenHandler(player, factory, shulker.getHoverName());
    }

    /**
     * 打开一个对话框
     */
    public static void openDialog(Player player, Dialog dialog) {
        player.openDialog(Holder.direct(dialog));
    }

    public static EntityPlayerActionPack getActionPack(ServerPlayer player) {
        return ((ServerPlayerInterface) player).getActionPack();
    }
}
