package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkCache;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.logger.Loggers;
import org.carpetorgaddition.logger.NetworkPacketLogger;
import org.carpetorgaddition.network.s2c.FakePlayerPathS2CPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FakePlayerGotoPath {
    private final EntityPlayerMPFake fakePlayer;
    private final BlockPos target;
    private final ArrayList<Vec3d> nodes = new ArrayList<>();
    private final World world;
    private int currentIndex;

    public FakePlayerGotoPath(EntityPlayerMPFake fakePlayer, BlockPos target) {
        this.fakePlayer = fakePlayer;
        this.world = fakePlayer.getWorld();
        this.target = target;
        this.initPath();
    }

    private void initPath() {
        BlockPos blockPos = this.fakePlayer.getBlockPos();
        int distance = 128;
        BlockPos from = blockPos.add(-distance, -distance, -distance);
        BlockPos to = blockPos.add(distance, distance, distance);
        ChunkCache chunkCache = new ChunkCache(this.world, from, to);
        LandPathNodeMaker maker = new LandPathNodeMaker();
        MobEntity entity = new MobEntity(EntityType.VILLAGER, this.world) {
            @Override
            protected void initGoals() {
                super.initGoals();
            }
        };
        Vec3d pos = this.fakePlayer.getPos();
        entity.setPosition(pos);
        this.nodes.add(pos);
        PathNodeNavigator navigator = new PathNodeNavigator(maker, 128);
        Path path = navigator.findPathToAny(chunkCache, entity, Set.of(target), distance, 0, 1F);
        if (path == null) {
            return;
        }
        for (int i = 0; i < path.getLength(); i++) {
            PathNode node = path.getNode(i);
            this.nodes.add(node.getBlockPos().toBottomCenterPos());
        }
        this.onStart();
    }

    public Vec3d getExpectedPos() {
        return this.nodes.get(this.currentIndex);
    }

    /**
     * @return 玩家是否到达了任意一个节点
     */
    public boolean arrivedAtAnyNode() {
        for (int i = this.currentIndex; i < this.nodes.size(); i++) {
            Vec3d current = this.nodes.get(i);
            Vec3d pos = this.fakePlayer.getPos();
            if (current.distanceTo(pos) <= 0.5) {
                this.currentIndex = i + 1;
                if (this.isFinished()) {
                    this.onStop();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * @return 是否完成寻路
     */
    public boolean isFinished() {
        return this.currentIndex >= this.nodes.size();
    }

    public List<Vec3d> getRenderNodes() {
        return this.nodes.stream().map(vec3d -> vec3d.add(0.0, 0.1, 0.0)).toList();
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
