package boat.carpetorgaddition.mixin.dialog;

import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.network.event.CustomClickActionContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin {
    @WrapOperation(method = "executeClickCommandsIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"))
    private void setPlayer(MinecraftServer instance, Identifier identifier, Optional<Tag> optional, Operation<Void> original, @Local(argsOnly = true) Player player) {
        if (player instanceof ServerPlayer) {
            try {
                CustomClickActionContext.CURRENT_PLAYER.set((ServerPlayer) player);
                original.call(instance, identifier, optional);
            } finally {
                CustomClickActionContext.CURRENT_PLAYER.remove();
            }
        } else {
            original.call(instance, identifier, optional);
        }
    }

    @WrapOperation(method = "executeClickCommandsIfPresent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;handleCustomClickAction(Lnet/minecraft/resources/Identifier;Ljava/util/Optional;)V"))
    private void setActionSource(MinecraftServer instance, Identifier identifier, Optional<Tag> optional, Operation<Void> original) {
        try {
            CustomClickActionContext.ACTION_SOURCE.set(ActionSource.SIGN);
            original.call(instance, identifier, optional);
        } finally {
            CustomClickActionContext.ACTION_SOURCE.remove();
        }
    }
}
