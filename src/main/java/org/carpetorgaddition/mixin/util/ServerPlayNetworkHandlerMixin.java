package org.carpetorgaddition.mixin.util;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {
    @WrapWithCondition(method = "onDisconnect", at = @At(value = "INVOKE", remap = false, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private boolean hide(Logger instance, String s, Object o1, Object o2) {
        return !CarpetOrgAdditionSettings.hiddenLoginMessages.getExternal();
    }
}
