package org.carpetorgaddition.client.renderer.waypoint;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.client.CarpetOrgAdditionClient;
import org.carpetorgaddition.client.renderer.WorldRenderer;
import org.carpetorgaddition.client.util.ClientKeyBindingUtils;
import org.carpetorgaddition.client.util.ClientMessageUtils;
import org.carpetorgaddition.client.util.ClientUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ClassCanBeRecord")
public class WaypointRenderer implements WorldRenderer {
    private final WaypointIcon waypoint;
    private final Vec3d target;
    private final String worldId;

    public WaypointRenderer(WaypointIcon waypoint, Vec3d target, World world) {
        this(waypoint, target, WorldUtils.getDimensionId(world));
    }

    public WaypointRenderer(WaypointIcon waypoint, Vec3d target, String worldId) {
        this.waypoint = waypoint;
        this.target = target;
        this.worldId = worldId;
    }

    /**
     * 绘制路径点
     */
    @Override
    public void render(WorldRenderContext renderContext) {
        if (ClientKeyBindingUtils.isPressed(CarpetOrgAdditionClient.CLEAR_WAYPOINT)
            && ClientUtils.getCurrentScreen() == null
            && WaypointIcon.HIGHLIGHT.equals(this.waypoint.getIcon())) {
            this.waypoint.stop();
        }
        MatrixStack matrixStack = renderContext.matrixStack();
        Camera camera = ClientUtils.getCamera();
        if (camera == null) {
            return;
        }
        Vec3d vec3d = this.getAdjustPos();
        if (vec3d == null) {
            return;
        }
        try {
            // 允许路径点透过方块渲染
            RenderSystem.disableDepthTest();
            // 绘制图标
            drawIcon(renderContext, matrixStack, vec3d, camera);
        } catch (RuntimeException e) {
            // 发送错误消息，然后停止渲染
            ClientMessageUtils.sendErrorMessage(e, "carpet.client.render.waypoint.error");
            CarpetOrgAddition.LOGGER.error("渲染{}路径点时遇到意外错误", this.waypoint.getName(), e);
            this.waypoint.clear();
        }
    }

    @Nullable
    private Vec3d getAdjustPos() {
        ClientPlayerEntity player = ClientUtils.getPlayer();
        if (this.target == null || this.worldId == null) {
            return null;
        }
        // 获取玩家所在维度ID
        String playerWorldId = WorldUtils.getDimensionId(FetcherUtils.getWorld(player));
        // 玩家和路径点在同一维度
        if (WorldUtils.equalsWorld(this.worldId, playerWorldId)) {
            return this.target;
        }
        Camera camera = ClientUtils.getCamera();
        // 玩家在主世界，路径点在下界，将路径点坐标换算成主世界坐标
        if (WorldUtils.isOverworld(playerWorldId) && WorldUtils.isTheNether(this.worldId)) {
            return new Vec3d(this.target.getX() * 8, camera.getPos().getY(), this.target.getZ() * 8);
        }
        // 玩家在下界，路径点在主世界，将路径点坐标换算成下界坐标
        if (WorldUtils.isTheNether(playerWorldId) && WorldUtils.isOverworld(this.worldId)) {
            return new Vec3d(this.target.getX() / 8, camera.getPos().getY(), this.target.getZ() / 8);
        }
        return null;
    }

    /**
     * 绘制路径点图标
     */
    private void drawIcon(WorldRenderContext context, MatrixStack matrixStack, Vec3d target, Camera camera) {
        VertexConsumerProvider consumers = context.consumers();
        RenderTickCounter tickCounter = context.tickCounter();
        this.waypoint.render(matrixStack, consumers, target, camera, tickCounter);
    }

    public boolean equalsTarget(WaypointRenderer renderer) {
        return this.target.equals(renderer.target) && WorldUtils.equalsWorld(this.worldId, renderer.worldId);
    }

    /**
     * @return 是否应该停止渲染
     */
    @Override
    public boolean shouldStop() {
        return this.waypoint.isDone();
    }

    /**
     * 设置该路径点消失
     */
    public void stop() {
        this.waypoint.stop();
    }

    public boolean isHighlight() {
        return WaypointIcon.HIGHLIGHT.equals(this.waypoint.getIcon());
    }

    public boolean isNavigator() {
        return WaypointIcon.NAVIGATOR.equals(this.waypoint.getIcon());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return waypoint == ((WaypointRenderer) o).waypoint;
    }

    @Override
    public int hashCode() {
        return waypoint.hashCode();
    }
}
