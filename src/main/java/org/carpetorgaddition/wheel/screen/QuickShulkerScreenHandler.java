package org.carpetorgaddition.wheel.screen;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.wheel.inventory.ContainerComponentInventory;

import java.util.function.Predicate;

public class QuickShulkerScreenHandler extends ShulkerBoxScreenHandler implements UnavailableSlotSyncInterface {
    private final int shulkerSlotIndex;
    private final ContainerComponentInventory inventory;
    private final ServerPlayerEntity player;
    /**
     * 是否可以继续使用快捷潜影盒
     */
    private final Predicate<PlayerEntity> predicate;
    private final ItemStack shulkerBox;

    public QuickShulkerScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            ContainerComponentInventory inventory,
            ServerPlayerEntity player,
            Predicate<PlayerEntity> predicate,
            ItemStack shulkerBox
    ) {
        super(syncId, playerInventory, inventory);
        this.inventory = inventory;
        this.player = player;
        this.predicate = predicate;
        this.shulkerBox = shulkerBox;
        if (canUseQuickShulker()) {
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
            if (button == FakePlayerUtils.PICKUP_RIGHT_CLICK) {
                ItemStack cursorStack = this.getCursorStack();
                // 光标物品是否可以放入潜影盒
                if (cursorStack.getItem().canBeNested()) {
                    ItemStack remaining = this.inventory.addStack(cursorStack);
                    this.setCursorStack(remaining);
                }
            }
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.shulkerBox.isEmpty()) {
            return false;
        }
        return canUseQuickShulker() && this.shulkerBox.getCount() == 1 && this.predicate.test(player) && super.canUse(player);
    }

    private boolean canUseQuickShulker() {
        return CarpetOrgAdditionSettings.quickShulker.get() || this.player instanceof EntityPlayerMPFake;
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

    public ItemStack getShulkerBox() {
        return this.shulkerBox;
    }
}
