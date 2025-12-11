package org.carpetorgaddition.mixin.debug;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyDeveloped
@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
    @Shadow
    private int nextTick;

    @Inject(method = "tick", at = @At("HEAD"))
    private void spawn(ServerLevel world, boolean spawnMonsters, CallbackInfo ci) {
        if (DebugSettings.phantomImmediatelySpawn.get()) {
            this.nextTick = 0;
        }
    }
}
