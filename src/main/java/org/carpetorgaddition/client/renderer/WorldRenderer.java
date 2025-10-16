package org.carpetorgaddition.client.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface WorldRenderer {
    void render(WorldRenderContext context);

    Object getKey();

    /**
     * 当前路径点被新路径点替换时调用
     *
     * @return 旧路径点是否可以自救
     */
    default boolean onUpdate(WorldRenderer renderer) {
        return false;
    }

    boolean shouldStop();
}
