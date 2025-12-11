package org.carpetorgaddition.mixin.rule;

import net.minecraft.world.item.trading.MerchantOffer;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantOffer.class)
public class TradeOfferMixin {

    //村民无限交易
    @Inject(method = "increaseUses", at = @At("HEAD"), cancellable = true)
    private void use(CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.villagerInfiniteTrade.get()) {
            ci.cancel();
        }
    }
}
