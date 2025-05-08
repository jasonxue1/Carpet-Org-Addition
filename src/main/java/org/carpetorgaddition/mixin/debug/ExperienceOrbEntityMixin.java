package org.carpetorgaddition.mixin.debug;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyDeveloped
@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbEntityMixin {
    @WrapOperation(method = "expensiveUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getClosestPlayer(Lnet/minecraft/entity/Entity;D)Lnet/minecraft/entity/player/PlayerEntity;"))
    private PlayerEntity expensiveUpdate(World instance, Entity entity, double v, Operation<PlayerEntity> original) {
        return DebugSettings.disableExperienceOrbSurround ? null : original.call(instance, entity, v);
    }
}
