package org.carpetorgaddition.mixin.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.wheel.GameProfileMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(UserCache.class)
public class UserCacheMixin {
    @Inject(method = "add(Lcom/mojang/authlib/GameProfile;)V", at = @At("HEAD"))
    private void add(GameProfile profile, CallbackInfo ci) {
        GameProfileMap.put(profile);
    }
}
