package org.carpetorgaddition.client.renderer.waypoint;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.client.util.ClientCommandUtils;
import org.carpetorgaddition.client.util.ClientUtils;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class NavigatorWaypoint extends Waypoint {
    private final int id;
    // TODO 新的网络包
    public static volatile boolean V2_PACKET = false;

    public NavigatorWaypoint(World world, Vec3d vec3d, int id) {
        super(world, vec3d, Waypoint.NAVIGATOR, 1, true);
        this.id = id;
    }

    @Override
    protected @Nullable Vec3d getRevisedPos() {
        Optional<Entity> optional = ClientUtils.getEntity(this.id);
        if (optional.isPresent()) {
            return optional.get().getPos();
        }
        return super.getRevisedPos();
    }

    @Override
    public void onClear() {
        ClientCommandUtils.sendCommand(CommandProvider.stopNavigate());
    }

    @Override
    public String getName() {
        return "Waypoint";
    }

    public int getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NavigatorWaypoint that = (NavigatorWaypoint) o;
        return V2_PACKET ? (id != -1 && this.id == that.id) : this.registryKey.equals(that.registryKey);
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}
