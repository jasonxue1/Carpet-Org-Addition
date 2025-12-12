package boat.carpetorgaddition.mixin.debug.accessor;

import boat.carpetorgaddition.debug.OnlyDeveloped;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@OnlyDeveloped
@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Accessor("leftPos")
    int getX();

    @Accessor("topPos")
    int getY();

    @Invoker("getHoveredSlot")
    Slot invokerGetSlotAt(double x, double y);
}
