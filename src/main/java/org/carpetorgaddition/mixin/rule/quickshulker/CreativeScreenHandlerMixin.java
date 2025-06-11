package org.carpetorgaddition.mixin.rule.quickshulker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;

@Environment(EnvType.CLIENT)
@Mixin(CreativeInventoryScreen.CreativeScreenHandler.class)
public abstract class CreativeScreenHandlerMixin extends ScreenHandlerMixin {
    @Override
    protected boolean canOpenShulker() {
        return false;
    }
}
