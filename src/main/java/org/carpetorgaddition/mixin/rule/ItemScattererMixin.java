package org.carpetorgaddition.mixin.rule;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemScatterer.class)
public class ItemScattererMixin {
    @Inject(method = "spawn(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)V", at = @At(value = "HEAD"), cancellable = true)
    private static void onStateReplaced(World world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.disableCreativeContainerDrops.get()) {
            ServerPlayerEntity player = CarpetOrgAdditionSettings.blockBreaking.get();
            if (player != null && player.isCreative()) {
                ci.cancel();
            }
        }
    }
}
