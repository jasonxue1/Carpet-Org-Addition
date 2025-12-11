package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Tuple;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DeathProtection;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.value.BetterTotemOfUndying;
import org.carpetorgaddition.util.InventoryUtils;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private final LivingEntity self = (LivingEntity) (Object) this;

    @Shadow
    @Nullable
    @SuppressWarnings("UnusedReturnValue")
    protected abstract Map<EquipmentSlot, ItemStack> collectEquipmentChanges();

    // 禁用伤害免疫
    @WrapOperation(method = "hurtServer", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = Opcodes.GETFIELD))
    private int setTimeUntilRegen(LivingEntity instance, Operation<Integer> original) {
        if (CarpetOrgAdditionSettings.disableDamageImmunity.get()) {
            return 0;
        }
        return original.call(instance);
    }

    // 不死图腾无敌时间
    @Inject(method = "checkTotemDeathProtection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V"))
    private void setInvincibleTime(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.totemOfUndyingInvincibleTime.get()) {
            this.self.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4));
        }
    }

    // 增强不死图腾
    @Definition(id = "itemStack", local = @Local(type = ItemStack.class, ordinal = 0))
    @Expression("itemStack != null")
    @ModifyExpressionValue(method = "checkTotemDeathProtection", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean tryUseTotem(boolean original, @Local(ordinal = 0) LocalRef<ItemStack> stackRef, @Local LocalRef<DeathProtection> componentRef) {
        if (original) {
            return true;
        }
        if (CarpetOrgAdditionSettings.betterTotemOfUndying.get() == BetterTotemOfUndying.VANILLA) {
            return false;
        }
        if (this.self instanceof Player player) {
            Tuple<ItemStack, DeathProtection> pair = pickTotem(player);
            if (pair == null || pair.getA().isEmpty()) {
                return false;
            }
            stackRef.set(pair.getA());
            componentRef.set(pair.getB());
            return true;
        }
        return false;
    }

    @Unique
    @Nullable
    // 从物品栏获取不死图腾
    private static Tuple<ItemStack, DeathProtection> pickTotem(Player playerEntity) {
        NonNullList<ItemStack> mainInventory = playerEntity.getInventory().getNonEquipmentItems();
        // 无论规则值是true还是shulker_box，都需要从物品栏获取物品
        for (ItemStack totemOfUndying : mainInventory) {
            DeathProtection component = totemOfUndying.get(DataComponents.DEATH_PROTECTION);
            if (component == null) {
                continue;
            }
            ItemStack itemStack = totemOfUndying.copy();
            totemOfUndying.shrink(1);
            return new Tuple<>(itemStack, component);
        }
        // 如果这里规则值为true，或者说规则值不是shulker_box，那就没有必要继续向下执行
        if (CarpetOrgAdditionSettings.betterTotemOfUndying.get() == BetterTotemOfUndying.INVENTORY) {
            return null;
        }
        for (ItemStack shulkerBox : mainInventory) {
            if (InventoryUtils.isOperableSulkerBox(shulkerBox)) {
                // 从潜影盒中拿取不死图腾
                // TODO 检查物品组件而不是物品类型
                ItemStack totemOfUndying = InventoryUtils.pickItemFromShulkerBox(shulkerBox, stack -> stack.is(Items.TOTEM_OF_UNDYING));
                // 潜影盒中可能没有不死图腾
                if (totemOfUndying.isEmpty()) {
                    continue;
                }
                ItemStack itemStack = totemOfUndying.copy();
                return new Tuple<>(itemStack, itemStack.get(DataComponents.DEATH_PROTECTION));
            }
        }
        return null;
    }

    @Unique
    protected void onPlayerBreakBlock() {
        this.collectEquipmentChanges();
    }
}