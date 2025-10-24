package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.wheel.BeaconRangeBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    // 大范围信标
    @WrapOperation(method = "applyPlayerEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getNonSpectatingEntities(Ljava/lang/Class;Lnet/minecraft/util/math/Box;)Ljava/util/List;"))
    private static List<PlayerEntity> box(World world, Class<PlayerEntity> aClass, Box box, Operation<List<PlayerEntity>> original, @Local(argsOnly = true) BlockPos pos) {
        BeaconRangeBox beaconRangeBox = new BeaconRangeBox(box);
        // 调整信标范围
        int range = CarpetOrgAdditionSettings.beaconRangeExpand.get();
        if (range != 0 && range <= 1024) {
            beaconRangeBox = beaconRangeBox.modify(range);
        }
        // 调整信标高度
        if (CarpetOrgAdditionSettings.beaconWorldHeight.get()) {
            beaconRangeBox = beaconRangeBox.worldHeight(world);
        }
        return original.call(world, aClass, beaconRangeBox);
    }
}
