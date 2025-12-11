package org.carpetorgaddition.debug.client.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface HudDebugRenderer {
    void render(GuiGraphics context, DeltaTracker tickCounter);
}
