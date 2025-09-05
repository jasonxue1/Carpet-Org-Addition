package org.carpetorgaddition.mixin.rule;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.screen.VillagerScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 村民立即补货
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
    @Unique
    private final VillagerEntity thisVillager = (VillagerEntity) (Object) this;

    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    // 打开村民物品栏
    @Inject(method = "interactMob", at = @At(value = "HEAD"), cancellable = true)
    private void clearVillagerInventory(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (CarpetOrgAdditionSettings.openVillagerInventory.get() && player.isSneaking()) {
            SimpleNamedScreenHandlerFactory screen =
                    new SimpleNamedScreenHandlerFactory((i, inventory, playerEntity)
                            -> new VillagerScreenHandler(i, inventory, thisVillager), thisVillager.getName());
            player.openHandledScreen(screen);
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    // 村民回血
    @Inject(method = "mobTick", at = @At("HEAD"))
    private void heal(CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.villagerHeal.get()) {
            long worldTime = FetcherUtils.getWorld(thisVillager).getTime();
            // 每四秒回一次血
            if (worldTime % 80 == 0) {
                thisVillager.heal(1.0F);
            }
        }
    }
}
