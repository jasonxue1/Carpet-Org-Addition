package org.carpetorgaddition.mixin.rule.quickshulker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ClickType;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.wheel.ItemStackPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "onStackClicked", at = @At("HEAD"), cancellable = true)
    private void onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        // 使用潜影盒单击物品
        if (CarpetOrgAdditionSettings.quickShulker.get()) {
            if (clickType == ClickType.LEFT || stack.isEmpty()) {
                return;
            }
            if (InventoryUtils.isOperableSulkerBox(stack)) {
                ItemStack slotStack = slot.getStack();
                if (slotStack.isEmpty()) {
                    ItemStack first = InventoryUtils.getFirstItemStack(stack);
                    if (slot.canInsert(first)) {
                        // 取出潜影盒中的物品
                        ItemStack picked = InventoryUtils.pickItemFromShulkerBox(stack, ItemStackPredicate.WILDCARD);
                        ItemStack inserted = slot.insertStack(picked);
                        if (!inserted.isEmpty()) {
                            // 物品可以被放入槽位，但是不能完全放入，例如在潜影盒中装入多个可以激活信标的物品，然后使用潜影盒单击信标槽位，
                            // 由于信标槽位中最多只能容纳1个物品，物品不能完全放入，因此这里的代码可能会被执行
                            ItemStack result = InventoryUtils.addItemToShulkerBox(stack, inserted);
                            insertToPlayerInventory(player, result);
                        }
                    }
                } else if (slotStack.getItem().canBeNested()) {
                    // 将物品放入潜影盒
                    int count = InventoryUtils.shulkerCanInsertItemCount(stack, slotStack);
                    ItemStack taken = slot.takeStackRange(slotStack.getCount(), count, player);
                    ItemStack result = InventoryUtils.addItemToShulkerBox(stack, taken);
                    insertToPlayerInventory(player, result);
                }
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private void insertToPlayerInventory(PlayerEntity player, ItemStack result) {
        if (result.isEmpty()) {
            return;
        }
        player.getInventory().insertStack(result);
        if (result.isEmpty()) {
            return;
        }
        player.dropItem(result, false);
    }

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = true)
    private void onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference reference, CallbackInfoReturnable<Boolean> cir) {
        // 使用物品单击潜影盒
        if (CarpetOrgAdditionSettings.quickShulker.get()) {
            if (clickType == ClickType.LEFT || stack.isEmpty()) {
                return;
            }
            // 要求槽位是可以取可以放的，避免玩家向工作台输出槽中的潜影盒中放入物品
            if (InventoryUtils.isOperableSulkerBox(stack) && slot.canTakePartial(player)) {
                ItemStack itemStack = InventoryUtils.addItemToShulkerBox(stack, reference.get());
                reference.set(itemStack);
                cir.setReturnValue(true);
            }
        }
    }
}
