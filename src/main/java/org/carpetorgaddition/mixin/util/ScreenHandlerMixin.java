package org.carpetorgaddition.mixin.util;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.carpetorgaddition.wheel.screen.AbstractPlayerInventoryScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    /**
     * 修复如果被合并的两个物品时同一个对象时，可能发生的物品复制问题
     *
     * @see AbstractPlayerInventoryScreenHandler#quickMove(PlayerEntity, int)
     */
    @WrapOperation(method = "insertItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areItemsAndComponentsEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean isEqual(ItemStack stack, ItemStack otherStack, Operation<Boolean> original) {
        if (AbstractPlayerInventoryScreenHandler.isQuickMovingItem.get() && stack == otherStack) {
            return false;
        }
        return original.call(stack, otherStack);
    }
}
