package org.carpetorgaddition.mixin.util;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.task.batch.BatchSpawnFakePlayerTask;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.inventory.FabricPlayerAccessManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    // 隐藏玩家登录登出的消息
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void broadcast(Text message, boolean overlay, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.hiddenLoginMessages.getExternal() || CarpetOrgAdditionSettings.hiddenLoginMessages.getInternal() || BatchSpawnFakePlayerTask.internalBatchSpawnHiddenMessage.get()) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "onPlayerConnect", at = @At(value = "INVOKE", remap = false, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V"))
    private boolean hide(Logger instance, String s, Object[] objects) {
        return !CarpetOrgAdditionSettings.hiddenLoginMessages.getInternal() && !BatchSpawnFakePlayerTask.internalBatchSpawnHiddenMessage.get();
    }

    /**
     * 如果被打开物品栏的玩家在物品栏被打开的期间上线，则自动关闭打开物品栏玩家的GUI
     */
    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void closePlayerInventory(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        MinecraftServer server = FetcherUtils.getServer(player);
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        FabricPlayerAccessManager accessManager = coordinator.getAccessManager();
        if (accessManager.hasViewers()) {
            PlayerConfigEntry entry = player.getPlayerConfigEntry();
            Set<ServerPlayerEntity> viewers = accessManager.getViewers(entry);
            if (viewers.isEmpty()) {
                return;
            }
            for (ServerPlayerEntity viewer : Set.copyOf(viewers)) {
                viewer.closeHandledScreen();
            }
        }
    }
}
