package org.carpetorgaddition.wheel.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.wheel.inventory.ServerPlayerInventory;

public class PlayerInventoryScreenHandler extends AbstractPlayerInventoryScreenHandler<ServerPlayerInventory> {
    private final ServerPlayerEntity player;

    public PlayerInventoryScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
        super(syncId, inventory, new ServerPlayerInventory(player));
        this.player = player;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // 玩家活着，并且玩家没有被删除
        return !this.player.isDead() && !this.player.isRemoved();
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.dropExcess(player);
    }
}
