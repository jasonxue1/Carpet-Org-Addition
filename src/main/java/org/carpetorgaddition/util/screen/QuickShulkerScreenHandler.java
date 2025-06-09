package org.carpetorgaddition.util.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.inventory.ContainerComponentInventory;

public class QuickShulkerScreenHandler extends ShulkerBoxScreenHandler implements UnavailableSlotSyncInterface {
    private final int shulkerSlotIndex;
    private final ContainerComponentInventory inventory;
    private final ItemStack shulkerBox;

    public QuickShulkerScreenHandler(int syncId, PlayerInventory playerInventory, ContainerComponentInventory inventory, ItemStack shulkerBox) {
        super(syncId, playerInventory, inventory);
        this.inventory = inventory;
        this.shulkerBox = shulkerBox;
        if (CarpetOrgAdditionSettings.quickShulker) {
            for (Slot slot : this.slots) {
                if (slot.getStack() == shulkerBox) {
                    this.shulkerSlotIndex = slot.id;
                    return;
                }
            }
            this.shulkerSlotIndex = -1;
        } else {
            throw new IllegalStateException("Quick shulker box not enabled");
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (MathUtils.isInRange(this.from(), this.to(), slotIndex)) {
            ItemStack stack = this.getCursorStack();
            ItemStack remaining = this.inventory.addStack(stack);
            this.setCursorStack(remaining);
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.shulkerBox.isEmpty()) {
            return false;
        }
        return CarpetOrgAdditionSettings.quickShulker && this.shulkerBox.getCount() == 1 && super.canUse(player);
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        super.onContentChanged(inventory);
    }

    @Override
    public int from() {
        return this.shulkerSlotIndex == -1 ? 0 : this.shulkerSlotIndex;
    }

    @Override
    public int to() {
        return this.shulkerSlotIndex;
    }
}
