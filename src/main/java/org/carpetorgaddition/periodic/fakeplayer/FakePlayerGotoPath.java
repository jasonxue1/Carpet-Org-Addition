package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.logger.Loggers;
import org.carpetorgaddition.logger.NetworkPacketLogger;
import org.carpetorgaddition.network.s2c.FakePlayerPathS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class FakePlayerGotoPath {
    private final EntityPlayerMPFake fakePlayer;
    private final BlockPos target;
    private final ArrayList<Vec3d> nodes = new ArrayList<>();
    private int currentIndex;
    private boolean canStart = false;

    public FakePlayerGotoPath(EntityPlayerMPFake fakePlayer, BlockPos target) {
        this.fakePlayer = fakePlayer;
        this.target = target;
    }

    public void straightTravel() {
        this.nodes.clear();
        this.currentIndex = 0;
        Vec3d pos = this.fakePlayer.getPos();
        this.nodes.add(pos);
        this.nodes.add(this.target.toBottomCenterPos());
        this.canStart = true;
        this.onStart();
    }

    public Vec3d getExpectedPos() {
        return this.nodes.get(this.currentIndex);
    }

    public void next() {
        this.currentIndex++;
        if (this.isFinished()) {
            this.onStop();
        }
    }

    public boolean isFinished() {
        return this.currentIndex >= this.nodes.size();
    }

    public boolean canStart() {
        return this.canStart;
    }

    public List<Vec3d> getRenderNodes() {
        return this.nodes.stream().map(vec3d -> vec3d.add(0.0, 0.1, 0.0)).toList();
    }

    public void invalidation() {
        this.nodes.clear();
        this.currentIndex = 0;
        this.canStart = false;
    }

    public EntityPlayerMPFake getFakePlayer() {
        return this.fakePlayer;
    }

    public void onStart() {
        if (LoggerRegister.fakePlayerPath) {
            NetworkPacketLogger logger = Loggers.getFakePlayerPathLogger();
            logger.sendPacket(() -> new FakePlayerPathS2CPacket(FakePlayerGotoPath.this));
        }
    }

    public void onStop() {
        if (LoggerRegister.fakePlayerPath) {
            NetworkPacketLogger logger = Loggers.getFakePlayerPathLogger();
            logger.sendPacket(() -> new FakePlayerPathS2CPacket(fakePlayer.getId(), List.of()));
        }
    }
}
