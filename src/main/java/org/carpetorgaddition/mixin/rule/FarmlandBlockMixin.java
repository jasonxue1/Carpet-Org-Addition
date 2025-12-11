package org.carpetorgaddition.mixin.rule;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public class FarmlandBlockMixin extends Block {
    public FarmlandBlockMixin(Properties settings) {
        super(settings);
    }

    // 耕地防踩踏
    @Inject(method = "fallOn", at = @At("HEAD"), cancellable = true)
    private void onLandedUpon(Level world, BlockState state, BlockPos pos, Entity entity, double fallDistance, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.farmlandPreventStepping.get()) {
            super.fallOn(world, state, pos, entity, fallDistance);
            ci.cancel();
        }
    }
}
