package boat.carpetorgaddition.mixin.debug.accessor;

import boat.carpetorgaddition.debug.OnlyDeveloped;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("UnusedReturnValue")
@OnlyDeveloped
@Mixin(Screen.class)
public interface ScreenAccessor {
    @Invoker("addRenderableOnly")
    <T extends Renderable> T putDrawable(T drawable);
}
