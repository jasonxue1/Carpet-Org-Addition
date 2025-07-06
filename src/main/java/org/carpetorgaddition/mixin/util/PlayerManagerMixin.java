package org.carpetorgaddition.mixin.util;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.wheel.inventory.OfflinePlayerInventory;
import org.carpetorgaddition.wheel.inventory.OfflinePlayerInventory.PlayerProfile;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    // 隐藏玩家登录登出的消息
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void broadcast(Text message, boolean overlay, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.hiddenLoginMessages.getExternal()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "onPlayerConnect", at = @At(value = "INVOKE", remap = false, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V"))
    private boolean hide(Logger instance, String s, Object[] objects) {
        return !CarpetOrgAdditionSettings.hiddenLoginMessages.getInternal();
    }

    /**
     * 如果被打开物品栏的玩家在物品栏被打开的期间上线，则自动关闭打开物品栏玩家的GUI
     */
    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void closePlayerInventory(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        if (OfflinePlayerInventory.INVENTORY_OPERATOR_PLAYERS.isEmpty()) {
            return;
        }
        GameProfile gameProfile = player.getGameProfile();
        PlayerProfile profile = new PlayerProfile(gameProfile);
        ServerPlayerEntity serverPlayerEntity = OfflinePlayerInventory.INVENTORY_OPERATOR_PLAYERS.get(profile);
        if (serverPlayerEntity != null) {
            serverPlayerEntity.closeHandledScreen();
        }
    }
}
