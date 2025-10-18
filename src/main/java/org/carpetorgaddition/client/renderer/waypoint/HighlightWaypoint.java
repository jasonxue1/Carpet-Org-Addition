package org.carpetorgaddition.client.renderer.waypoint;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.client.CarpetOrgAdditionClient;
import org.carpetorgaddition.client.util.ClientKeyBindingUtils;
import org.carpetorgaddition.client.util.ClientUtils;

public class HighlightWaypoint extends Waypoint {
    public HighlightWaypoint(World world, Vec3d vec3d, long duration, boolean persistent) {
        super(world, vec3d, Waypoint.HIGHLIGHT, duration, persistent);
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider consumers, Camera camera, RenderTickCounter tickCounter) {
        if (ClientKeyBindingUtils.isPressed(CarpetOrgAdditionClient.CLEAR_WAYPOINT) && ClientUtils.getCurrentScreen() == null) {
            this.stop();
        }
        super.render(matrixStack, consumers, camera, tickCounter);
    }

    @Override
    public String getName() {
        return "Highlight";
    }
}
