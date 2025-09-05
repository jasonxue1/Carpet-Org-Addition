package org.carpetorgaddition.mixin.rule;

import net.minecraft.entity.ExperienceOrbEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.FetcherUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin {
    @Shadow
    private int orbAge;

    @Shadow
    private int pickingCount;

    @Shadow
    public abstract int getValue();

    @Shadow
    protected abstract void setValue(int value);

    @Unique
    private final ExperienceOrbEntity thisEntity = (ExperienceOrbEntity) (Object) this;

    @Inject(method = "isMergeable(Lnet/minecraft/entity/ExperienceOrbEntity;II)Z", at = @At("HEAD"), cancellable = true)
    private static void isMergeable(ExperienceOrbEntity orb, int seed, int amount, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.experienceOrbMerge.get()) {
            boolean combine = ((ExperienceOrbEntityMixin) (Object) orb).combine();
            if (combine) {
                cir.setReturnValue(!orb.isRemoved() && orb.getValue() + amount <= Short.MAX_VALUE);
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "merge", at = @At("HEAD"), cancellable = true)
    private void merge(ExperienceOrbEntity other, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.experienceOrbMerge.get() && this.combine()) {
            int sum = this.getValue() * this.pickingCount + other.getValue() * ((ExperienceOrbEntityMixin) (Object) other).pickingCount;
            if (sum > Short.MAX_VALUE) {
                ci.cancel();
                return;
            }
            this.setValue(sum);
            this.orbAge = Math.min(this.orbAge, ((ExperienceOrbEntityMixin) (Object) other).orbAge);
            this.pickingCount = 1;
            other.discard();
            ci.cancel();
        }
    }

    /**
     * 此方案可以提高玩家吸收经验球的效率，但是经验球过多时合并的效率不如原版。因此每隔一段时间切换一次合并方式，两种方式结合起来使用。
     */
    @Unique
    private boolean combine() {
        long time = FetcherUtils.getWorld(thisEntity).getTime();
        return Math.sin(time / 10.0) > 0;
    }
}
