package org.carpetorgaddition.mixin.rule.disableanvilexpensive;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {
    @Shadow
    @Final
    private Property levelCost;
    // TODO 当前字段是否同时被两个线程访问？
    @Unique
    private boolean shouldOutput;

    @WrapMethod(method = "updateResult")
    private void updateResult(Operation<Void> original) {
        try {
            this.shouldOutput = true;
            original.call();
        } finally {
            this.shouldOutput = false;
        }
    }

    @WrapOperation(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getCount()I", ordinal = 1))
    private int markInvalid(ItemStack instance, Operation<Integer> original) {
        int call = original.call(instance);
        if (call > 1) {
            // 堆叠的物品不应该被附魔
            this.shouldOutput = false;
        }
        return call;
    }

    @Inject(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/Property;get()I", ordinal = 1))
    private void updateResult(CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.disableExpensive.get() && this.shouldOutput) {
            if (this.levelCost.get() >= 40) {
                this.levelCost.set(39);
            }
        }
    }
}
