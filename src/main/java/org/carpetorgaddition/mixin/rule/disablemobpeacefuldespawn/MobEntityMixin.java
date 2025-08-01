package org.carpetorgaddition.mixin.rule.disablemobpeacefuldespawn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEntity.class)
public class MobEntityMixin {
    @Unique
    private final MobEntity thisMob = (MobEntity) (Object) this;

    // 禁止特定生物在和平模式下被清除
    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;isAllowedInPeaceful()Z"))
    private boolean isDisallowedInPeaceful(EntityType<?> mob, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.disableMobPeacefulDespawn.get() && (thisMob.isPersistent() || thisMob.cannotDespawn())) {
            return true;
        }
        return original.call(mob);
    }
}
