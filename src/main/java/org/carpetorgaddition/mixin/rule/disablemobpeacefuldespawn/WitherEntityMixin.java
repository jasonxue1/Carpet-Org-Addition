package org.carpetorgaddition.mixin.rule.disablemobpeacefuldespawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherEntity.class)
public class WitherEntityMixin {
    @Unique
    private final WitherEntity thisWither = (WitherEntity) (Object) this;

    // 禁止特定生物在和平模式下被清除（凋灵）
    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;isAllowedInPeaceful()Z"))
    private boolean isDisallowedInPeaceful(EntityType<?> instance, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.disableMobPeacefulDespawn.get() && (thisWither.isPersistent() || thisWither.cannotDespawn())) {
            return true;
        }
        return original.call(instance);
    }
}
