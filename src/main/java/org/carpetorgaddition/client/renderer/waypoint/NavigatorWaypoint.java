package org.carpetorgaddition.client.renderer.waypoint;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.client.util.ClientCommandUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.wheel.provider.CommandProvider;

public class NavigatorWaypoint extends Waypoint {
    /**
     * 插值动画进度，上次设置目标后到现在经过的时间
     */
    private double progress = 0d;

    public NavigatorWaypoint(RegistryKey<World> registryKey, Vec3d vec3d) {
        super(registryKey, vec3d, Waypoint.NAVIGATOR, 1, true);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider consumers, Camera camera, RenderTickCounter tickCounter) {
        // 计算帧间时间增量
        double delta = this.tickDelta - this.lastTickDelta;
        if (delta < 0) {
            // 处理时间回绕
            delta = delta - Math.floor(delta);
        }
        this.progress += delta;
        super.render(matrixStack, consumers, camera, tickCounter);
    }

    @Override
    protected Vec3d getInterpolation() {
        if (this.progress >= 1d) {
            return super.getInterpolation();
        }
        return MathUtils.approach(this.lastTarget, this.getTarget(), this.progress);
    }

    @Override
    public void setTarget(RegistryKey<World> registryKey, Vec3d vec3d) {
        if (registryKey.equals(this.registryKey)) {
            this.lastTarget = this.getTarget();
            this.progress = 0d;
        }
        super.setTarget(registryKey , vec3d);
    }

    @Override
    public void requestServerToStop() {
        ClientCommandUtils.sendCommand(CommandProvider.stopNavigate());
    }

    @Override
    public String getName() {
        return "Waypoint";
    }
}
