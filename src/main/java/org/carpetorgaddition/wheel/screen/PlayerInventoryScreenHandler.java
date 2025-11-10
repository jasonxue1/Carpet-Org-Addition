package org.carpetorgaddition.wheel.screen;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
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
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (CarpetOrgAddition.isDebugDevelopment() && MathUtils.isInRange(this.from(), this.to(), slotIndex) && this.player instanceof EntityPlayerMPFake fakePlayer) {
            PlayerStorageInventory inventory = new PlayerStorageInventory(fakePlayer);
            inventory.sort();
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
