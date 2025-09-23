package org.carpetorgaddition.mixin.util;

import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.wheel.GameProfileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UserCache.class)
public class UserCacheMixin {
    @Inject(method = "add(Lnet/minecraft/server/PlayerConfigEntry;)V", at = @At("HEAD"))
    private void add(PlayerConfigEntry entry, CallbackInfo ci) {
        GameProfileCache.put(entry);
    }
}
