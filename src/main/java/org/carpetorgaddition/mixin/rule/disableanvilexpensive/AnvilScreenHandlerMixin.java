package org.carpetorgaddition.mixin.rule.disableanvilexpensive;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AnvilScreenHandler;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilScreenHandler.class)
public class AnvilScreenHandlerMixin {
    @WrapOperation(method = "updateResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getCount()I", ordinal = 1))
    private int markInvalid(ItemStack instance, Operation<Integer> original, @Local(ordinal = 0) LocalIntRef ref) {
        int call = original.call(instance);
        int value = CarpetOrgAdditionSettings.setAnvilCostLimit.get();
        if (value == -1 || call <= 1) {
            return call;
        }
        ref.set(value + 1);
        return 1;
    }

    @ModifyExpressionValue(method = "updateResult", at = @At(value = "CONSTANT", args = "intValue=40", ordinal = 2))
    private int updateResult(int original) {
        // 重命名物品的最大等级仍为39级
        int value = CarpetOrgAdditionSettings.setAnvilCostLimit.get();
        if (value == -1) {
            return original;
        }
        return value;
    }
}
