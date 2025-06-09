package org.carpetorgaddition.mixin.rule.quickshulker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference reference, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.quickShulker) {
            if (clickType == ClickType.LEFT) {
                return;
            }
            if (InventoryUtils.isShulkerBoxItem(stack)) {
                // 使用物品去单击潜影盒
                if (stack.getCount() == 1) {
                    ItemStack itemStack = InventoryUtils.addItemToContainer(stack, reference.get());
                    reference.set(itemStack);
                    cir.setReturnValue(true);
                }
            } else if (InventoryUtils.isShulkerBoxItem(reference.get())) {
                // 使用潜影盒去单击物品
                if (reference.get().getCount() == 1) {
                    ItemStack slotStack = slot.getStack();
                    ItemStack itemStack;
                    if (slotStack.isEmpty()) {
                        // 取出潜影盒中的物品
                        itemStack = InventoryUtils.pickItemFromShulkerBox(reference.get(), ItemStackPredicate.WILDCARD);
                    } else {
                        // 将物品放入潜影盒
                        itemStack = InventoryUtils.addItemToContainer(reference.get(), slotStack);
                    }
                    slot.setStack(itemStack);
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
