package org.carpetorgaddition.mixin.debug;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyDeveloped
@Mixin(FishingBobberEntity.class)
public class FishingBobberEntityMixin {
    @Shadow
    private int waitCountdown;

    @Inject(method = "tickFishingLogic", at = @At("HEAD"))
    private void tick(BlockPos pos, CallbackInfo ci) {
        int fixedTime = DebugSettings.fixedFishingHookTime.get();
        if (fixedTime == -1) {
            return;
        }
        this.waitCountdown = Math.min(this.waitCountdown, fixedTime);
    }

    @WrapOperation(method = "tickFishingLogic", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;hasRain(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean hasRain(World instance, BlockPos blockPos, Operation<Boolean> original) {
        return DebugSettings.fixedFishingHookTime.get() != -1 && original.call(instance, blockPos);
    }

    @WrapOperation(method = "tickFishingLogic", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isSkyVisible(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean isSkyVisible(World instance, BlockPos blockPos, Operation<Boolean> original) {
        return DebugSettings.fixedFishingHookTime.get() == -1 || original.call(instance, blockPos);
    }
}
