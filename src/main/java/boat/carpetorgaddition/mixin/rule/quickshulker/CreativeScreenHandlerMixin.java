package boat.carpetorgaddition.mixin.rule.quickshulker;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;

@Environment(EnvType.CLIENT)
@Mixin(CreativeModeInventoryScreen.ItemPickerMenu.class)
public abstract class CreativeScreenHandlerMixin extends ScreenHandlerMixin {
    @Override
    protected boolean canOpenShulker() {
        return false;
    }
}
