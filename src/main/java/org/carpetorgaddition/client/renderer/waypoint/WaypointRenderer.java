package org.carpetorgaddition.client.renderer.waypoint;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.client.CarpetOrgAdditionClient;
import org.carpetorgaddition.client.renderer.WorldRenderer;
import org.carpetorgaddition.client.renderer.WorldRendererManager;
import org.carpetorgaddition.client.util.ClientKeyBindingUtils;
import org.carpetorgaddition.client.util.ClientMessageUtils;
import org.carpetorgaddition.client.util.ClientUtils;

@SuppressWarnings("ClassCanBeRecord")
public class WaypointRenderer implements WorldRenderer {
    private final Waypoint waypoint;

    public WaypointRenderer(Waypoint waypoint) {
        this.waypoint = waypoint;
    }

    /**
     * 绘制路径点
     */
    @Override
    public void render(WorldRenderContext context) {
        if (ClientKeyBindingUtils.isPressed(CarpetOrgAdditionClient.CLEAR_WAYPOINT)
            && ClientUtils.getCurrentScreen() == null
            && Waypoint.HIGHLIGHT.equals(this.waypoint.getIcon())) {
            this.waypoint.stop();
        }
        MatrixStack matrixStack = context.matrixStack();
        Camera camera = ClientUtils.getCamera();
        if (camera == null) {
            return;
        }
        try {
            // 允许路径点透过方块渲染
            RenderSystem.disableDepthTest();
            // 绘制图标
            drawIcon(context, matrixStack, camera);
        } catch (RuntimeException e) {
            // 发送错误消息，然后停止渲染
            ClientMessageUtils.sendErrorMessage(e, "carpet.client.render.waypoint.error");
            CarpetOrgAddition.LOGGER.error("An unexpected error occurred while rendering waypoint '{}'", this.waypoint.getName(), e);
            this.clear();
        }
    }

    /**
     * 绘制路径点图标
     */
    private void drawIcon(WorldRenderContext context, MatrixStack matrixStack, Camera camera) {
        VertexConsumerProvider consumers = context.consumers();
        RenderTickCounter tickCounter = context.tickCounter();
        this.waypoint.render(matrixStack, consumers, camera, tickCounter);
    }

    /**
     * @return 是否应该停止渲染
     */
    @Override
    public boolean shouldStop() {
        return this.waypoint.isDone();
    }

    @Override
    public boolean onUpdate(WorldRenderer renderer) {
        if (renderer instanceof WaypointRenderer waypointRenderer) {
            if (this.waypoint.onUpdate(waypointRenderer.waypoint)) {
                this.stop();
                return true;
            }
            return false;
        }
        throw new IllegalArgumentException();
    }

    /**
     * 设置该路径点消失
     */
    public void stop() {
        this.waypoint.stop();
    }

    public boolean isHighlight() {
        return Waypoint.HIGHLIGHT.equals(this.waypoint.getIcon());
    }

    public boolean isNavigator() {
        return Waypoint.NAVIGATOR.equals(this.waypoint.getIcon());
    }

    @Override
    public Object getKey() {
        return this.waypoint.getIcon();
    }

    private void clear() {
        WorldRendererManager.remove(WaypointRenderer.class, renderer -> this.waypoint.getIcon().equals(renderer.waypoint.getIcon()));
        this.waypoint.onClear();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WaypointRenderer that = (WaypointRenderer) o;
        return this.waypoint.equals(that.waypoint);
    }

    @Override
    public int hashCode() {
        return this.waypoint.hashCode();
    }
}
