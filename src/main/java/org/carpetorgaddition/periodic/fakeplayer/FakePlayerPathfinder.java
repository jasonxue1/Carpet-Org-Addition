package org.carpetorgaddition.periodic.fakeplayer;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkCache;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.logger.Loggers;
import org.carpetorgaddition.logger.NetworkPacketLogger;
import org.carpetorgaddition.network.s2c.FakePlayerPathS2CPacket;
import org.carpetorgaddition.util.MathUtils;

import java.util.*;
import java.util.function.Supplier;

public class FakePlayerPathfinder {
    private final EntityPlayerMPFake fakePlayer;
    private final Supplier<Optional<BlockPos>> target;
    private final ArrayList<Vec3d> nodes = new ArrayList<>();
    private final World world;
    private final DummyEntity entity;
    private int currentIndex;
    /**
     * 无需改变移动方向的时间
     */
    private int directTravelTime = 0;
    /**
     * 更新路径剩余游戏刻
     */
    private int updateTime;
    /**
     * 是否没有路能到达目标位置
     */
    private boolean noRoad = false;
    private BlockPos previous;
    /**
     * 重试寻路的次数
     */
    private int retryCount;
    public static final int FOLLOW_RANGE = 48;

    public FakePlayerPathfinder(EntityPlayerMPFake fakePlayer, BlockPos blockPos) {
        this(fakePlayer, () -> Optional.of(blockPos));
    }

    public FakePlayerPathfinder(EntityPlayerMPFake fakePlayer, Entity entity) {
        this(fakePlayer, () -> Optional.of(entity.getBlockPos()));
    }

    public FakePlayerPathfinder(EntityPlayerMPFake fakePlayer, Supplier<Optional<BlockPos>> supplier) {
        this.target = supplier;
        this.fakePlayer = fakePlayer;
        this.world = fakePlayer.getWorld();
        this.entity = new DummyEntity(this.world);
        this.pathfinding();
    }

    public void tick() {
        Optional<BlockPos> optional = this.target.get();
        if (optional.isEmpty()) {
            if (this.isFinished()) {
                return;
            }
            this.stop();
            return;
        }
        BlockPos blockPos = optional.get();
        if (!Objects.equals(this.previous, blockPos)) {
            this.pathfinding();
        }
        this.previous = blockPos;
        Vec3d pos = this.fakePlayer.getPos();
        this.directTravelTime--;
        if (this.updateTime > 0) {
            this.updateTime--;
        }
        // 是否还没有接近目标
        boolean farAway = blockPos.toBottomCenterPos().distanceTo(pos) > 1.5;
        if (this.updateTime == 0 && farAway) {
            // 更新路径
            this.noRoad = false;
            this.pathfinding();
            this.directTravelTime = 2;
        }
        if (this.isFinished()) {
            if (farAway) {
                // 已经无路可走了，避免每个游戏刻都查询路径
                if (this.noRoad) {
                    return;
                }
                // 没有到达目标位置，立即更新路径
                this.pathfinding();
            }
            return;
        }
        Vec3d current = this.getCurrentNode();
        boolean onGround = this.fakePlayer.isOnGround();
        if (onGround) {
            if (this.directTravelTime <= 0) {
                this.fakePlayer.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, current);
            }
        } else {
            // 玩家在从一格高的方块上下来，有时会尝试回到上一个节点
            this.directTravelTime = 2;
        }
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
        // 玩家到达了当前节点
        if (this.arrivedAtAnyNode()) {
            return;
        }
        actionPack.setForward(1F);
        if (onGround) {
            this.jump(current, pos);
        }
    }

    private void jump(Vec3d current, Vec3d pos) {
        double horizontal = MathUtils.horizontalDistance(current, pos);
        double vertical = MathUtils.verticalDistance(current, pos);
        // 玩家可以直接走向方块，不需要跳跃
        if (vertical <= fakePlayer.getAttributeValue(EntityAttributes.GENERIC_STEP_HEIGHT)) {
            return;
        }
        // 当前位置比玩家位置低，不需要跳跃
        if (current.getY() < pos.getY()) {
            return;
        }
        // 跳跃高度可能受多种因素影响，但这里不考虑它
        if (horizontal < 1.0 && vertical < 1.25) {
            this.fakePlayer.jump();
        }
    }

    private double length() {
        if (this.nodes.size() <= 1) {
            return 0.0;
        }
        return this.nodes.getFirst().distanceTo(this.nodes.getLast());
    }

    public EntityPlayerMPFake getFakePlayer() {
        return this.fakePlayer;
    }

    private void pathfinding() {
        this.retryCount++;
        this.updateTime = 100;
        this.nodes.clear();
        this.currentIndex = 0;
        Optional<BlockPos> optional = this.target.get();
        if (optional.isEmpty()) {
            return;
        }
        BlockPos blockPos = this.fakePlayer.getBlockPos();
        BlockPos from = blockPos.add(-FOLLOW_RANGE, -FOLLOW_RANGE, -FOLLOW_RANGE);
        BlockPos to = blockPos.add(FOLLOW_RANGE, FOLLOW_RANGE, FOLLOW_RANGE);
        ChunkCache chunkCache = new ChunkCache(this.world, from, to);
        LandPathNodeMaker maker = new LandPathNodeMaker();
        Vec3d pos = this.fakePlayer.getPos();
        this.entity.setPosition(pos);
        this.nodes.add(pos);
        PathNodeNavigator navigator = new PathNodeNavigator(maker, FOLLOW_RANGE * 16);
        Path path = navigator.findPathToAny(chunkCache, this.entity, Set.of(optional.get()), FOLLOW_RANGE, 0, 1F);
        if (path == null) {
            return;
        }
        for (int i = 0; i < path.getLength(); i++) {
            PathNode node = path.getNode(i);
            this.nodes.add(node.getBlockPos().toBottomCenterPos());
        }
        this.onStart();
        if (this.length() < 1.0) {
            this.noRoad = true;
        }
    }

    public Vec3d getCurrentNode() {
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
                if (i > 0) {
                    this.retryCount = 0;
                }
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

    public void stop() {
        this.currentIndex = this.nodes.size();
        this.onStop();
    }

    public void onStart() {
        if (LoggerRegister.fakePlayerPath) {
            NetworkPacketLogger logger = Loggers.getFakePlayerPathLogger();
            logger.sendPacket(() -> new FakePlayerPathS2CPacket(this));
        }
    }

    public void onStop() {
        if (LoggerRegister.fakePlayerPath) {
            NetworkPacketLogger logger = Loggers.getFakePlayerPathLogger();
            logger.sendPacket(() -> new FakePlayerPathS2CPacket(fakePlayer.getId(), List.of()));
        }
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
        actionPack.setForward(0F);
    }

    /**
     * @return 目标位置是否是不可到达的
     */
    public boolean isInaccessible() {
        if (this.isFinished()) {
            return true;
        }
        if (this.target.get().isEmpty()) {
            return true;
        }
        return this.retryCount > 5;
    }

    /**
     * 一个占位实体，没有实际作用
     */
    private static class DummyEntity extends MobEntity {
        protected DummyEntity(World world) {
            super(EntityType.VILLAGER, world);
        }
    }
}
