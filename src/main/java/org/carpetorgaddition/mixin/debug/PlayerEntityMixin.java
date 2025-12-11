package org.carpetorgaddition.mixin.debug;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
@Mixin(value = Player.class, priority = 1001)
public class PlayerEntityMixin {
    @Unique
    private final Player thisPlayer = (Player) (Object) this;

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void openInventory(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (hand == InteractionHand.OFF_HAND) {
            return;
        }
        ProductionEnvironmentError.assertDevelopmentEnvironment();
        if (DebugSettings.openFakePlayerInventory.get() && entity instanceof EntityPlayerMPFake fakePlayer) {
            if (thisPlayer instanceof ServerPlayer player) {
                CommandUtils.execute(player, CommandProvider.openPlayerInventory(fakePlayer));
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
