package org.carpetorgaddition.client.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

public interface WorldRenderer {
    void render(WorldRenderContext context);

    Object getKey();

    /**
     * 当前渲染器被新渲染器替换时调用
     *
     * @return 旧渲染器是否可以自救
     */
    default boolean onUpdate(WorldRenderer renderer) {
        return false;
    }

    boolean shouldStop();
}
