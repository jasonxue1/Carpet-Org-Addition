package org.carpetorgaddition.mixin.util;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.EntityTrackingStatus;
import org.carpetorgaddition.util.GenericUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Shadow
    @Final
    private ServerEntityManager<Entity> entityManager;

    /**
     * 修复假玩家自动登录时因等待区块加载而卡住的问题
     *
     * @see <a href="https://github.com/fcsailboat/Carpet-Org-Addition/issues/51">假人的autologin为true时导致无法进入存档</a>
     */
    @WrapOperation(method = "method_72080", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;isChunkLoaded(J)Z"))
    private boolean loadChunk(ServerWorld world, long chunkPosAsLong, Operation<Boolean> original) {
        if (original.call(world, chunkPosAsLong)) {
            return true;
        }
        if (GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.get() != null) {
            ChunkPos chunkPos = new ChunkPos(chunkPosAsLong);
            this.entityManager.updateTrackingStatus(chunkPos, EntityTrackingStatus.TICKING);
        }
        return false;
    }
}
