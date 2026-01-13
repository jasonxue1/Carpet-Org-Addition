package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.CustomRuleControls;
import boat.carpetorgaddition.rule.RuleUtils;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntityMixin {
    @Shadow
    public abstract FoodData getFoodData();

    @Shadow
    public abstract boolean isSpectator();

    @Shadow
    protected abstract void touch(Entity entity);

    @Unique
    private final Player thisPlayer = (Player) (Object) this;

    // 血量不满时也可以进食
    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void canEat(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.healthNotFullCanEat.get() && thisPlayer.getHealth() < thisPlayer.getMaxHealth() - 0.3 // -0.3：可能生命值不满但是显示的心满了
            && this.getFoodData().getSaturationLevel() <= 5) {
            cir.setReturnValue(true);
        }
    }

    // 快速设置假玩家合成
    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void interact(Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (this.isSpectator()) {
            return;
        }
        switch (CarpetOrgAdditionSettings.quickSettingFakePlayerCraft.get()) {
            case FALSE:
                break;
            case SNEAKING:
                if (!thisPlayer.isShiftKeyDown()) {
                    break;
                }
            case TRUE:
                if (openQuickCraftGui(entity)) {
                    cir.setReturnValue(InteractionResult.SUCCESS);
                }
            default: {
            }
        }
    }

    /**
     * 打开合成配方设置GUI
     *
     * @return 是否执行成功
     */
    @Unique
    private boolean openQuickCraftGui(Entity entity) {
        Optional<Function<EntityPlayerMPFake, String>> optional = getOpenQuickCraftGuiCommand(thisPlayer.getMainHandItem());
        if (optional.isEmpty()) {
            return false;
        }
        if (thisPlayer instanceof ServerPlayer player && entity instanceof EntityPlayerMPFake fakePlayer) {
            CommandUtils.execute(player, optional.get().apply(fakePlayer));
        }
        // 客户端虽然不执行命令，但仍要返回true，否则不会显示摆动手动画
        return true;
    }

    // 获取打开GUI所需要的命令
    @Unique
    private Optional<Function<EntityPlayerMPFake, String>> getOpenQuickCraftGuiCommand(ItemStack itemStack) {
        PermissionSet predicate = thisPlayer.permissions();
        boolean canUseCommand = CommandUtils.canUseCommand(predicate, CarpetOrgAdditionSettings.commandPlayerAction.get());
        if (canUseCommand) {
            if (itemStack.isEmpty()) {
                return Optional.empty();
            }
            // 工作台
            if (itemStack.is(Items.CRAFTING_TABLE)) {
                return Optional.of(CommandProvider::openPlayerCraftGui);
            }
            // 切石机
            if (itemStack.is(Items.STONECUTTER)) {
                return Optional.of(CommandProvider::openPlayerStonecuttingGui);
            }
        }
        return Optional.empty();
    }

    // 最大方块交互距离
    @Inject(method = "blockInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (ServerUtils.getWorld(thisPlayer).isClientSide() && !CarpetOrgAdditionSettings.maxBlockPlaceDistanceSyncClient.get()) {
            return;
        }
        if (RuleUtils.isDefaultDistance()) {
            return;
        }
        cir.setReturnValue(RuleUtils.getPlayerMaxInteractionDistance());
    }

    // 实体交互距离
    @Inject(method = "entityInteractionRange", at = @At("HEAD"), cancellable = true)
    private void getEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (CarpetOrgAdditionSettings.maxBlockPlaceDistanceReferToEntity.get()) {
            cir.setReturnValue(RuleUtils.getPlayerMaxInteractionDistance());
        }
    }

    // 玩家死亡产生的掉落物不会自然消失
    @WrapOperation(method = "dropEquipment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;dropAll()V"))
    private void drop(Inventory inventory, Operation<Void> original) {
        if (CarpetOrgAdditionSettings.playerDropsNotDespawning.get()) {
            for (ItemStack itemStack : inventory) {
                if (!itemStack.isEmpty()) {
                    ItemEntity itemEntity = inventory.player.drop(itemStack, true, false);
                    if (itemEntity == null) {
                        continue;
                    }
                    // 设置掉落物不消失
                    itemEntity.setUnlimitedLifetime();
                }
            }
        } else {
            // 掉落物正常消失
            original.call(inventory);
        }
    }

    @Inject(method = "getDestroySpeed", at = @At(value = "HEAD"))
    private void getBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        if (CarpetOrgAdditionSettings.applyToolEffectsImmediately.get()) {
            this.collectEquipmentChanges();
        }
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private void pickupItem(CallbackInfo ci, @Local AABB box) {
        if (this.thisPlayer instanceof ServerPlayer player) {
            int range = CustomRuleControls.ITEM_PICKUP_RANGE_EXPAND.getRuleValue(player);
            if (range == 0) {
                return;
            }
            double minX = box.minX - range;
            double minY = box.minY - range;
            double minZ = box.minZ - range;
            double maxX = box.maxX + range;
            double maxY = box.maxY + range;
            double maxZ = box.maxZ + range;
            AABB expand = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            List<Entity> list = ServerUtils.getWorld(this.thisPlayer)
                    .getEntities(this.thisPlayer, expand)
                    .stream()
                    .filter(entity -> !entity.isRemoved())
                    .filter(entity -> entity.getType() == EntityType.ITEM)
                    .toList();
            for (Entity entity : list) {
                this.touch(entity);
            }
        }
    }
}
