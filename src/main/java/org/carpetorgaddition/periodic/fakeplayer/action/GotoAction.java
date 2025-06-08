package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerPathfinder;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class GotoAction extends AbstractPlayerAction {
    private final FakePlayerPathfinder pathfinder;
    private final TargetType targetType;
    private final Text displayName;
    private final Supplier<Optional<BlockPos>> target;

    public GotoAction(@NotNull EntityPlayerMPFake fakePlayer, BlockPos blockPos) {
        super(fakePlayer);
        this.target = () -> Optional.of(blockPos);
        this.pathfinder = FakePlayerPathfinder.of(this::getFakePlayer, this.target);
        this.targetType = TargetType.BLOCK;
        this.displayName = TextProvider.blockPos(blockPos);
    }

    public GotoAction(@NotNull EntityPlayerMPFake fakePlayer, Entity entity) {
        super(fakePlayer);
        this.target = new EntityTracker(this::getFakePlayer, entity.getWorld(), entity);
        this.pathfinder = FakePlayerPathfinder.of(this::getFakePlayer, this.target);
        this.targetType = TargetType.ENTITY;
        this.displayName = entity.getDisplayName();
    }

    @Override
    protected void tick() {
        if (this.pathfinder.isFinished()) {
            Vec3d pos = this.getFakePlayer().getPos();
            this.target.get().ifPresent(blockPos -> {
                if (blockPos.toBottomCenterPos().distanceTo(pos) > 3) {
                    pathfinder.pathfinding();
                }
            });
        }
        this.pathfinder.tick();
    }

    @Override
    public ArrayList<MutableText> info() {
        Text name = this.getFakePlayer().getDisplayName();
        String key = this.targetType.getTranslateKey();
        MutableText text = TextBuilder.translate(key, name, displayName);
        ArrayList<MutableText> list = new ArrayList<>();
        list.add(text);
        return list;
    }

    @Override
    public JsonObject toJson() {
        // 不保存玩家数据
        return new JsonObject();
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.goto");
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

    private enum TargetType {
        BLOCK("carpet.commands.playerAction.info.goto.block"),
        ENTITY("carpet.commands.playerAction.info.goto.entity");

        private final String translateKey;

        TargetType(String key) {
            this.translateKey = key;
        }

        private String getTranslateKey() {
            return this.translateKey;
        }
    }

    private static class EntityTracker implements Supplier<Optional<BlockPos>> {
        /**
         * 用来获取外部类的玩家，玩家可能会因切换维度导致地址值发生变化
         */
        private final Supplier<EntityPlayerMPFake> supplier;
        /**
         * 玩家当前跟随的实体
         */
        @NotNull
        private Entity entity;
        /**
         * 玩家当前的目标
         */
        @NotNull
        private BlockPos target;
        /**
         * 当前跟随实体的UUID
         */
        private final UUID entityUuid;
        /**
         * 目标实体是否已被不可逆的清除
         */
        private boolean isEntityDestroy;
        /**
         * 上一次目标更新时间
         */
        private long lastUpdateTime;

        private EntityTracker(Supplier<EntityPlayerMPFake> supplier, World world, Entity entity) {
            this.supplier = supplier;
            this.entity = entity;
            this.entityUuid = entity.getUuid();
            this.target = entity.getBlockPos();
            this.lastUpdateTime = world.getTime();
        }

        @Override
        public Optional<BlockPos> get() {
            // 实体已经被删除，或不在同一维度
            EntityPlayerMPFake fakePlayer = this.supplier.get();
            if (this.isEntityDestroy) {
                return Optional.empty();
            }
            // 每隔3秒更新一次
            long time = this.getWorld().getTime();
            if (time - this.lastUpdateTime < 60) {
                // 不到3秒，返回之前的位置
                return Optional.of(this.target);
            }
            this.lastUpdateTime = time;
            this.target = this.entity.getBlockPos();
            MinecraftServer server = GenericUtils.getServer(fakePlayer);
            // 实体已被删除
            if (this.entity.isRemoved()) {
                switch (this.entity) {
                    case ServerPlayerEntity player -> {
                        ServerPlayerEntity entity = server.getPlayerManager().getPlayer(player.getUuid());
                        if (entity == null) {
                            // 玩家退出了游戏
                            this.isEntityDestroy = true;
                            return Optional.empty();
                        } else {
                            // 玩家切换了维度
                            this.entity = entity;
                        }
                    }
                    case LivingEntity livingEntity -> {
                        // 实体已死亡
                        if (livingEntity.isDead()) {
                            this.isEntityDestroy = true;
                            return Optional.empty();
                        }
                        Entity.RemovalReason reason = livingEntity.getRemovalReason();
                        if (reason == null) {
                            // 不应该会执行到这里
                            break;
                        }
                        // 实体被不可逆的删除
                        if (reason.shouldDestroy()) {
                            this.isEntityDestroy = true;
                            return Optional.empty();
                        }
                        // 实体被可逆的删除，例如区块卸载，跨越维度
                        Entity entity = GenericUtils.getEntity(server, this.entityUuid);
                        if (entity != null) {
                            this.entity = entity;
                        }
                    }
                    default -> {
                        this.isEntityDestroy = true;
                        return Optional.empty();
                    }
                }
            }
            if (fakePlayer.getWorld() == this.getWorld()) {
                return Optional.of(this.target);
            }
            // 玩家与目标实体不在同一维度
            return Optional.empty();
        }

        private World getWorld() {
            return this.entity.getWorld();
        }
    }
}
