package org.carpetorgaddition.mixin.rule;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
    // 绑定诅咒无效化
    @Inject(method = "has", at = @At("HEAD"), cancellable = true)
    private static void hasAnyEnchantmentsWith(ItemStack stack, DataComponentType<?> componentType, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.bindingCurseInvalidation.get() && EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE.equals(componentType)) {
            cir.setReturnValue(false);
        }
    }
}
