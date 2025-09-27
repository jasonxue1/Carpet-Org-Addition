package org.carpetorgaddition.client.renderer;

import org.carpetorgaddition.client.renderer.substitute.WorldRenderContext;

public interface WorldRenderer {
    void render(WorldRenderContext context);

    default boolean shouldStop() {
        return false;
    }
}
