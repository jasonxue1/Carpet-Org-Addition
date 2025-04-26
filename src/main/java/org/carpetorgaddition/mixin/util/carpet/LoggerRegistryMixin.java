package org.carpetorgaddition.mixin.util.carpet;

import carpet.CarpetServer;
import carpet.logging.LoggerRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.network.s2c.LoggerUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoggerRegistry.class, remap = false)
public class LoggerRegistryMixin {
    // 记录器取消订阅事件
    @Inject(method = "unsubscribePlayer", at = @At(value = "INVOKE", target = "Ljava/util/Map;size()I"))
    private static void unsubscribePlayer(String playerName, String logName, CallbackInfo ci) {
        if (CarpetServer.scriptServer == null) {
            return;
        }
        ServerPlayerEntity player = CarpetServer.minecraft_server.getPlayerManager().getPlayer(playerName);
        if (player != null) {
            ServerPlayNetworking.send(player, new LoggerUpdateS2CPacket(logName, null, true));
        }
    }
}
