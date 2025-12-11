package org.carpetorgaddition.mixin.debug;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyDeveloped
@Mixin(FishingHook.class)
public class FishingBobberEntityMixin {
    @Shadow
    private int timeUntilLured;

    @Inject(method = "catchingFish", at = @At("HEAD"))
    private void tick(BlockPos pos, CallbackInfo ci) {
        int fixedTime = DebugSettings.fixedFishingHookTime.get();
        if (fixedTime == -1) {
            return;
        }
        this.timeUntilLured = Math.min(this.timeUntilLured, fixedTime);
    }

    @WrapOperation(method = "catchingFish", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isRainingAt(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hasRain(Level instance, BlockPos blockPos, Operation<Boolean> original) {
        return DebugSettings.fixedFishingHookTime.get() != -1 && original.call(instance, blockPos);
    }

    @WrapOperation(method = "catchingFish", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean isSkyVisible(Level instance, BlockPos blockPos, Operation<Boolean> original) {
        return DebugSettings.fixedFishingHookTime.get() == -1 || original.call(instance, blockPos);
    }
}
