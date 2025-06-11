package org.carpetorgaddition.mixin.debug;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.carpetorgaddition.exception.ProductionEnvironmentError;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyDeveloped
@Mixin(value = PlayerEntity.class, priority = 1001)
public class PlayerEntityMixin {
    @Unique
    private final PlayerEntity thisPlayer = (PlayerEntity) (Object) this;

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void openInventory(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (hand == Hand.OFF_HAND) {
            return;
        }
        ProductionEnvironmentError.assertDevelopmentEnvironment();
        if (DebugSettings.openFakePlayerInventory && entity instanceof EntityPlayerMPFake fakePlayer) {
            if (thisPlayer instanceof ServerPlayerEntity player) {
                CommandUtils.execute(player, CommandProvider.openPlayerInventory(fakePlayer));
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
