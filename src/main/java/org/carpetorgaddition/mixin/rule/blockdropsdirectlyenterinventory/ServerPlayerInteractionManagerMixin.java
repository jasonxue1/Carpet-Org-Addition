package org.carpetorgaddition.mixin.rule.blockdropsdirectlyenterinventory;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean tryBreakBlock(BlockPos pos, Operation<Boolean> original) {
        try {
            CarpetOrgAdditionSettings.blockBreaking.set(this.player);
            return original.call(pos);
        } finally {
            CarpetOrgAdditionSettings.blockBreaking.remove();
        }
    }
}
