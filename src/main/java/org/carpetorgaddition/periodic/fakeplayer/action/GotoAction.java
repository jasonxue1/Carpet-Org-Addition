package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerPathfinder;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class GotoAction extends AbstractPlayerAction {
    private final FakePlayerPathfinder pathfinder;
    private final TargetType targetType;
    private final Component displayName;
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
        this.target = new EntityTracker(this::getFakePlayer, FetcherUtils.getWorld(entity), entity);
        this.pathfinder = FakePlayerPathfinder.of(this::getFakePlayer, this.target);
        this.targetType = TargetType.ENTITY;
        this.displayName = entity.getDisplayName();
    }

    @Override
    protected void tick() {
        if (this.pathfinder.isFinished()) {
            switch (this.targetType) {
                case BLOCK -> {
                    return;
                }
                case ENTITY -> this.target.get().ifPresent(blockPos -> {
                    EntityTracker tracker = (EntityTracker) this.target;
                    Vec3 pos = FetcherUtils.getFootPos(tracker.entity);
                    if (blockPos.getBottomCenter().distanceTo(pos) > 3) {
                        tracker.update();
                        pathfinder.pathfinding();
                    }
                });
            }
        }
        this.pathfinder.tick();
    }

    @Override
    public List<Component> info() {
        Component name = this.getFakePlayer().getDisplayName();
        String key = this.targetType.getTranslateKey();
        Component text = TextBuilder.translate(key, name, displayName);
        ArrayList<Component> list = new ArrayList<>();
        list.add(text);
        return list;
    }

    @Override
    public JsonObject toJson() {
        // 不保存玩家数据
        return new JsonObject();
    }

    @Override
    public Component getDisplayName() {
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

    public enum TargetType {
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

    public static class EntityTracker implements Supplier<Optional<BlockPos>> {
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

        private EntityTracker(Supplier<EntityPlayerMPFake> supplier, Level world, Entity entity) {
            this.supplier = supplier;
            this.entity = entity;
            this.entityUuid = entity.getUUID();
            this.target = entity.blockPosition();
            this.lastUpdateTime = world.getGameTime();
        }

        @Override
        public Optional<BlockPos> get() {
            // 实体已经被删除，或不在同一维度
            EntityPlayerMPFake fakePlayer = this.supplier.get();
            if (this.isEntityDestroy) {
                return Optional.empty();
            }
            // 每隔3秒更新一次
            long time = this.getWorld().getGameTime();
            if (time - this.lastUpdateTime < 60) {
                // 不到3秒，返回之前的位置
                return Optional.of(this.target);
            }
            this.lastUpdateTime = time;
            this.target = this.entity.blockPosition();
            MinecraftServer server = FetcherUtils.getServer(fakePlayer);
            // 实体已被删除
            if (this.entity.isRemoved()) {
                switch (this.entity) {
                    case ServerPlayer player -> {
                        ServerPlayer entity = server.getPlayerList().getPlayer(player.getUUID());
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
                        if (livingEntity.isDeadOrDying()) {
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
                        Optional<Entity> optional = GenericUtils.getEntity(server, this.entityUuid);
                        optional.ifPresent(value -> this.entity = value);
                    }
                    default -> {
                        this.isEntityDestroy = true;
                        return Optional.empty();
                    }
                }
            }
            if (FetcherUtils.getWorld(fakePlayer) == this.getWorld()) {
                return Optional.of(this.target);
            }
            // 玩家与目标实体不在同一维度
            return Optional.empty();
        }

        private void update() {
            this.target = this.entity.blockPosition();
        }

        private Level getWorld() {
            return FetcherUtils.getWorld(this.entity);
        }
    }
}
