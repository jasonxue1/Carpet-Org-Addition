package org.carpetorgaddition.mixin.rule;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin {
    @Shadow
    private boolean headsDropped;

    @Shadow
    public abstract boolean isCharged();

    //和平的苦力怕
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void setTarget(LivingEntity target, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.peacefulCreeper.get() && target instanceof PlayerEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "onKilledOther", at = @At("HEAD"))
    private void onHeadDropped(ServerWorld world, LivingEntity other, DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        // 闪电苦力怕同时炸死多个生物时每个都掉落头颅
        if (CarpetOrgAdditionSettings.superChargedCreeper.get()) {
            this.headsDropped = false;
        }
        // 玩家被闪电苦力怕炸死掉落头颅
        if (CarpetOrgAdditionSettings.playerDropHead.get() && this.isCharged() && !this.headsDropped && other instanceof ServerPlayerEntity player) {
            ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
            itemStack.set(DataComponentTypes.PROFILE, new ProfileComponent(player.getGameProfile()));
            player.dropStack(player.getEntityWorld(), itemStack);
        }
    }
}