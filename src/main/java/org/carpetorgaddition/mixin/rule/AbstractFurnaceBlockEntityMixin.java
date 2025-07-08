package org.carpetorgaddition.mixin.rule;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public class AbstractFurnaceBlockEntityMixin {
    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private static void dropExperience(ServerWorld world, Vec3d pos, int multiplier, float experience, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.disableFurnaceDropExperience.get()) {
            ci.cancel();
        }
    }
}
