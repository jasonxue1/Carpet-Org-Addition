package org.carpetorgaddition.mixin.rule.disableanvilexpensive;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @WrapMethod(method = "set")
    private <T> T set(ComponentType<? super T> type, T value, Operation<T> original) {
        if (CarpetOrgAdditionSettings.disableExpensive.get() && type == DataComponentTypes.REPAIR_COST) {
            return original.call(type, 39);
        }
        return original.call(type, value);
    }
}
