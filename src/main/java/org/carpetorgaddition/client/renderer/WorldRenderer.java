package org.carpetorgaddition.client.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface WorldRenderer {
    void render(WorldRenderContext context);

    Object getIdentityValue();

    default boolean onUpdate(WorldRenderer renderer) {
        return false;
    }

    default boolean shouldStop() {
        return false;
    }
}
