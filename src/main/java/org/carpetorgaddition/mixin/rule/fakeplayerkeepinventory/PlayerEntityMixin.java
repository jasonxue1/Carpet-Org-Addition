package org.carpetorgaddition.mixin.rule.fakeplayerkeepinventory;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.FakePlayerDamageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Unique
    private final PlayerEntity thisPlayer = (PlayerEntity) (Object) this;

    @Inject(method = "applyDamage", at = @At("HEAD"))
    private void recordDamageSource(DamageSource source, float amount, CallbackInfo ci) {
        if (thisPlayer instanceof EntityPlayerMPFake) {
            FakePlayerDamageTracker.recordDamage(thisPlayer, source);
        }
    }

    @Unique
    private boolean shouldKeepInventory() {
        if (!CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() || !(thisPlayer instanceof EntityPlayerMPFake)) {
            return false;
        }

        if (!CarpetOrgAdditionSettings.fakePlayerConditionalKeepInventory.get()) {
            return true;
        }

        return checkDamageChainForKeepInventory();
    }

    @Unique
    private boolean checkDamageChainForKeepInventory() {
        var recentDamageSources = FakePlayerDamageTracker.getRecentDamageSources(thisPlayer);

        for (var source : recentDamageSources) {
            if (source.getAttacker() instanceof PlayerEntity) {
                return true;
            }

            if (source.getSource() instanceof PlayerEntity) {
                return true;
            }

            if (source == thisPlayer.getDamageSources().outOfWorld()) {
                return true;
            }
        }

        return false;
    }

    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void dropInventory(CallbackInfo ci) {
        if (shouldKeepInventory()) {
            ci.cancel();
        }
    }

    @Inject(method = "getXpToDrop", at = @At("HEAD"), cancellable = true)
    private void getXpToDrop(CallbackInfoReturnable<Integer> cir) {
        if (shouldKeepInventory()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void clearOldDamageSources(CallbackInfo ci) {
        if (thisPlayer instanceof EntityPlayerMPFake) {
            if (thisPlayer.getWorld().getTime() % 200 == 0) {
                FakePlayerDamageTracker.clearDamageSources(thisPlayer);
            }
        }
    }
}