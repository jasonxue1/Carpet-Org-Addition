package boat.carpetorgaddition.client.renderer.waypoint;

import boat.carpetorgaddition.client.CarpetOrgAdditionClient;
import boat.carpetorgaddition.client.util.ClientKeyBindingUtils;
import boat.carpetorgaddition.client.util.ClientUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// TODO 下个快照版本测试是否正常
public class HighlightWaypoint extends Waypoint {
    public HighlightWaypoint(Level world, Vec3 vec3d, long duration, boolean persistent) {
        super(world, vec3d, Waypoint.HIGHLIGHT, duration, persistent);
    }

    @Override
    public void render(PoseStack matrixStack, MultiBufferSource consumers, Camera camera, DeltaTracker tickCounter) {
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
