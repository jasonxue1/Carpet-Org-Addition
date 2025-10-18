package org.carpetorgaddition.client.renderer.waypoint;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.client.util.ClientCommandUtils;
import org.carpetorgaddition.wheel.provider.CommandProvider;

public class NavigatorWaypoint extends Waypoint {
    public NavigatorWaypoint(String worldId, Vec3d vec3d) {
        this(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId)), vec3d);
    }

    public NavigatorWaypoint(RegistryKey<World> registryKey, Vec3d vec3d) {
        super(registryKey, vec3d, Waypoint.NAVIGATOR, 1, true);
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
