package org.carpetorgaddition.client.renderer.waypoint;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HighlightWaypoint extends Waypoint {
    public HighlightWaypoint(World world, Vec3d vec3d, long duration, boolean persistent) {
        super(world, vec3d, Waypoint.HIGHLIGHT, duration, persistent);
    }

    @Override
    public String getName() {
        return "Highlight";
    }
}
