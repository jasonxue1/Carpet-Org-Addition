package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerPathfinder;
import org.carpetorgaddition.util.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

public class GotoAction extends AbstractPlayerAction {
    private final FakePlayerPathfinder pathfinder;

    public GotoAction(@NotNull EntityPlayerMPFake fakePlayer, BlockPos blockPos) {
        super(fakePlayer);
        this.pathfinder = FakePlayerPathfinder.of(fakePlayer, () -> Optional.of(blockPos));
    }

    public GotoAction(@NotNull EntityPlayerMPFake fakePlayer, Entity entity) {
        super(fakePlayer);
        World world = fakePlayer.getWorld();
        this.pathfinder = FakePlayerPathfinder.of(fakePlayer, new EntityTracker(world, entity));
    }

    @Override
    protected void tick() {
        this.pathfinder.tick();
    }

    @Override
    public ArrayList<MutableText> info() {
        // TODO 玩家动作信息
        return new ArrayList<>();
    }

    @Override
    public JsonObject toJson() {
        // 不保存玩家数据
        return new JsonObject();
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.empty();
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        // 不序列化
        return ActionSerializeType.STOP;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public void onStop() {
        this.pathfinder.onStop();
    }

    private static class EntityTracker implements Supplier<Optional<BlockPos>> {
        private final World world;
        private final Entity entity;
        @NotNull
        private BlockPos target;
        private long lastUpdateTime;

        private EntityTracker(World world, Entity entity) {
            this.world = world;
            this.entity = entity;
            this.target = entity.getBlockPos();
            this.lastUpdateTime = world.getTime();
        }

        @Override
        public Optional<BlockPos> get() {
            if (this.entity.isRemoved()) {
                return Optional.empty();
            }
            long time = this.world.getTime();
            if (time - this.lastUpdateTime >= 100) {
                this.lastUpdateTime = time;
                this.target = this.entity.getBlockPos();
            }
            return Optional.of(this.target);
        }
    }
}
