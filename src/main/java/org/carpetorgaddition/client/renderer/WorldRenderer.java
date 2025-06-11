package org.carpetorgaddition.client.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface WorldRenderer {
    void render(WorldRenderContext context);

    default boolean shouldStop() {
        return false;
    }
}
