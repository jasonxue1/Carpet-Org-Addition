package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.EnchantmentUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

//强化引雷
@Mixin(ThrownTrident.class)
public abstract class TridentEntityMixin extends AbstractArrow {
    private TridentEntityMixin(EntityType<? extends AbstractArrow> entityType, Level world) {
        super(entityType, world);
    }


    // 击中实体
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostAttackEffectsWithItemSourceOnBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V"))
    private void onEnhityHit(ServerLevel world, Entity target, DamageSource damageSource, ItemStack weapon, Consumer<Item> breakCallback, Operation<Void> original) {
        original.call(world, target, damageSource, weapon, breakCallback);
        spwnLighining(world, target.blockPosition());
    }

    // 击中避雷针
    @Inject(method = "hitBlockEnchantmentEffects", at = @At(value = "TAIL"))
    private void onBlockHitEnchantmentEffects(ServerLevel world, BlockHitResult blockHitResult, ItemStack weaponStack, CallbackInfo ci) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        if (world.getBlockState(blockPos).is(Blocks.LIGHTNING_ROD)) {
            spwnLighining(world, blockPos.above());
        }
    }

    // 生成闪电
    @Unique
    private void spwnLighining(ServerLevel world, BlockPos blockPos) {
        // 只需要在晴天生成，因为雷雨天的引雷三叉戟本来就会生成闪电
        if (world.isRaining() && world.isThundering()) {
            return;
        }
        // TODO 引雷忽略了维度和露天，这是一开始就设计成这样的吗？
        boolean hasChanneling = EnchantmentUtils.hasEnchantment(world, Enchantments.CHANNELING, this.getWeaponItem());
        if (CarpetOrgAdditionSettings.channelingIgnoreWeather.get() && Level.isInSpawnableBounds(blockPos) && hasChanneling) {
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.spawn(world, blockPos, EntitySpawnReason.TRIGGERED);
            if (lightning == null) {
                return;
            }
            if (this.getOwner() instanceof ServerPlayer player) {
                lightning.setCause(player);
            }
        }
    }
}
