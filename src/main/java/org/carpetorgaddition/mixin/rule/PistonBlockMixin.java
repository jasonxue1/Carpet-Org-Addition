package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBaseBlock.class)
public class PistonBlockMixin {
    /**
     * 防止修改硬度的基岩被活塞推动
     * {@link AbstractBlockStateMixin}
     */
    @WrapOperation(method = "isPushable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private static float canMove(BlockState instance, BlockGetter blockView, BlockPos blockPos, Operation<Float> original) {
        if (instance.getBlock() == Blocks.BEDROCK) {
            return -1;
        }
        return original.call(instance, blockView, blockPos);
    }
}
