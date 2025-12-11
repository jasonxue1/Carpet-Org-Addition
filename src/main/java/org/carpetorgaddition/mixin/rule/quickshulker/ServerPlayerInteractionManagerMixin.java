package org.carpetorgaddition.mixin.rule.quickshulker;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.ScreenUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerInteractionManagerMixin {
    @Inject(method = "useItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"), cancellable = true)
    private void interactItem(ServerPlayer player, Level world, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (stack.isEmpty()) {
            return;
        }
        if (CarpetOrgAdditionSettings.quickShulker.get() && InventoryUtils.isShulkerBoxItem(stack) && stack.getCount() == 1) {
            ScreenUtils.openShulkerScreenHandler(player, stack);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
