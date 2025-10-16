package org.carpetorgaddition.mixin.rule.respawnblocksexplode;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//禁止重生方块爆炸
@Mixin(BedBlock.class)
public class BedBlockMixin {
    //禁止床爆炸
    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BedRule;errorMessage()Ljava/util/Optional;"), cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (CarpetOrgAdditionSettings.disableRespawnBlocksExplode.get()) {
            MessageUtils.sendMessageToHud(player, TextBuilder.translate("carpet.rule.message.disableRespawnBlocksExplode"));
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
