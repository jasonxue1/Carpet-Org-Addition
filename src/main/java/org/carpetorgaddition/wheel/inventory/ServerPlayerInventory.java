package org.carpetorgaddition.wheel.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerPlayerInventory extends AbstractCustomSizeInventory {
    private final ServerPlayerEntity player;
    private final PlayerInventory inventory;

    public ServerPlayerInventory(ServerPlayerEntity player) {
        this.player = player;
        this.inventory = player.getInventory();
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    protected Inventory getInventory() {
        return this.inventory;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        // 玩家活着，并且玩家没有被删除
        return !this.player.isDead() && !this.player.isRemoved();
    }
}
