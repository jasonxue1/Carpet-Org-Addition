package org.carpetorgaddition.wheel.screen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.NonNull;

// 假玩家末影箱GUI
public class PlayerEnderChestScreenHandler extends ChestMenu {
    private final ServerPlayer player;

    public PlayerEnderChestScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
        super(MenuType.GENERIC_9x3, syncId, inventory, player.getEnderChestInventory(), 3);
        this.player = player;
    }

    //假玩家死亡时，自动关闭GUI
    @Override
    public boolean stillValid(@NonNull Player player) {
        if (this.player == null) {
            return false;
        }
        return !this.player.isRemoved();
    }
}
