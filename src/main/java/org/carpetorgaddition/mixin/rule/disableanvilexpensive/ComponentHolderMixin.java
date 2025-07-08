package org.carpetorgaddition.mixin.rule.disableanvilexpensive;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ComponentHolder.class)
public interface ComponentHolderMixin {
    @SuppressWarnings("unchecked")
    @WrapMethod(method = "get")
    private <T> T get(ComponentType<? extends T> type, Operation<T> original) {
        T call = original.call(type);
        if (call != null && CarpetOrgAdditionSettings.disableExpensive.get() && type == DataComponentTypes.REPAIR_COST) {
            return (T) Integer.valueOf(Math.min((Integer) call, 39));
        }
        return call;
    }

    @SuppressWarnings("unchecked")
    @WrapMethod(method = "getOrDefault")
    private <T> T getOrDefault(ComponentType<? extends T> type, T fallback, Operation<T> original) {
        T call = original.call(type, fallback);
        // 参数fallback可能也为null
        if (call != null && CarpetOrgAdditionSettings.disableExpensive.get() && type == DataComponentTypes.REPAIR_COST) {
            return (T) Integer.valueOf(Math.min((Integer) call, 39));
        }
        return call;
    }
}
