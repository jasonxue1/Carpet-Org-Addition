package org.carpetorgaddition.util.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

// 假玩家末影箱GUI
public class PlayerEnderChestScreenHandler extends GenericContainerScreenHandler {
    private final ServerPlayerEntity player;

    public PlayerEnderChestScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, inventory, player.getEnderChestInventory(), 3);
        this.player = player;
    }

    //假玩家死亡时，自动关闭GUI
    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.player == null) {
            return false;
        }
        return !this.player.isRemoved();
    }
}
