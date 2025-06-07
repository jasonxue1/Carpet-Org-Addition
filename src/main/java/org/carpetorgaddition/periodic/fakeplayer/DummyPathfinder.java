package org.carpetorgaddition.periodic.fakeplayer;

import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 寻路的空实现，这是一个单例
 */
public final class DummyPathfinder implements FakePlayerPathfinder {
    public static final DummyPathfinder EMPTY = new DummyPathfinder();

    private DummyPathfinder() {
    }

    @Override
    public void tick() {
    }

    @Override
    public double length() {
        return 0;
    }

    @Override
    public Vec3d getCurrentNode() {
        return Vec3d.ZERO;
    }

    @Override
    public boolean arrivedAtAnyNode() {
        return false;
    }

    @Override
    public boolean backToBeforeNode() {
        return true;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public List<Vec3d> getRenderNodes() {
        return List.of();
    }

    @Override
    public int getSyncEntityId() {
        return -1;
    }

    @Override
    public void stop() {
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onStop() {
    }

    @Override
    public boolean isInvalid() {
        return true;
    }

    @Override
    public boolean isInaccessible() {
        return true;
    }
}
