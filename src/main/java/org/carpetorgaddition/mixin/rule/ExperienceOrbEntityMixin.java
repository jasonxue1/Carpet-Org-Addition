package org.carpetorgaddition.mixin.rule;

import net.minecraft.entity.ExperienceOrbEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbEntityMixin {
    @Shadow
    private int amount;

    @Shadow
    private int orbAge;

    @Inject(method = "isMergeable(Lnet/minecraft/entity/ExperienceOrbEntity;II)Z", at = @At("HEAD"), cancellable = true)
    private static void isMergeable(ExperienceOrbEntity orb, int seed, int amount, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.experienceOrbMerge) {
            cir.setReturnValue(!orb.isRemoved());
        }
    }

    @Inject(method = "merge", at = @At("HEAD"), cancellable = true)
    private void merge(ExperienceOrbEntity other, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.experienceOrbMerge) {
            this.amount = this.amount + other.getExperienceAmount();
            this.orbAge = Math.min(this.orbAge, ((ExperienceOrbEntityMixin) (Object) other).orbAge);
            other.discard();
            ci.cancel();
        }
    }
}
