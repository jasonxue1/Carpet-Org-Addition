package org.carpetorgaddition.mixin.rule.disableanvilexpensive.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
    @ModifyExpressionValue(method = "drawForeground", at = @At(value = "CONSTANT", args = "intValue=40"))
    private int disableExpensive(int original) {
        int value = CarpetOrgAdditionSettings.setAnvilCostLimit.get();
        return value == -1 ? original : value;
    }
}
