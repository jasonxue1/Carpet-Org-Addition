package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
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
    protected abstract Map<EquipmentSlot, ItemStack> getEquipmentChanges();

    // 禁用伤害免疫
    @WrapOperation(method = "damage", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;timeUntilRegen:I", opcode = Opcodes.GETFIELD))
    private int setTimeUntilRegen(LivingEntity instance, Operation<Integer> original) {
        if (CarpetOrgAdditionSettings.disableDamageImmunity.get()) {
            return 0;
        }
        return original.call(instance);
    }

    // 不死图腾无敌时间
    @Inject(method = "tryUseDeathProtector", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;sendEntityStatus(Lnet/minecraft/entity/Entity;B)V"))
    private void setInvincibleTime(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.totemOfUndyingInvincibleTime.get()) {
            this.self.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4));
        }
    }

    // 增强不死图腾
    @SuppressWarnings("LocalMayBeArgsOnly")
    @Definition(id = "itemStack", local = @Local(type = ItemStack.class, ordinal = 0))
    @Expression("itemStack != null")
    @ModifyExpressionValue(method = "tryUseDeathProtector", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean tryUseTotem(boolean original, @Local(ordinal = 0) LocalRef<ItemStack> stackRef, @Local LocalRef<DeathProtectionComponent> componentRef) {
        if (original) {
            return true;
        }
        if (CarpetOrgAdditionSettings.betterTotemOfUndying.get() == BetterTotemOfUndying.VANILLA) {
            return false;
        }
        if (this.self instanceof PlayerEntity player) {
            Pair<ItemStack, DeathProtectionComponent> pair = pickTotem(player);
            if (pair == null || pair.getLeft().isEmpty()) {
                return false;
            }
            stackRef.set(pair.getLeft());
            componentRef.set(pair.getRight());
            return true;
        }
        return false;
    }

    @Unique
    @Nullable
    // 从物品栏获取不死图腾
    private static Pair<ItemStack, DeathProtectionComponent> pickTotem(PlayerEntity playerEntity) {
        DefaultedList<ItemStack> mainInventory = playerEntity.getInventory().main;
        // 无论规则值是true还是shulker_box，都需要从物品栏获取物品
        for (ItemStack totemOfUndying : mainInventory) {
            DeathProtectionComponent component = totemOfUndying.get(DataComponentTypes.DEATH_PROTECTION);
            if (component == null) {
                continue;
            }
            ItemStack itemStack = totemOfUndying.copy();
            totemOfUndying.decrement(1);
            return new Pair<>(itemStack, component);
        }
        // 如果这里规则值为true，或者说规则值不是shulker_box，那就没有必要继续向下执行
        if (CarpetOrgAdditionSettings.betterTotemOfUndying.get() == BetterTotemOfUndying.INVENTORY) {
            return null;
        }
        for (ItemStack shulkerBox : mainInventory) {
            if (InventoryUtils.isOperableSulkerBox(shulkerBox)) {
                // 从潜影盒中拿取不死图腾
                ItemStack totemOfUndying = InventoryUtils.pickItemFromShulkerBox(shulkerBox, stack -> stack.isOf(Items.TOTEM_OF_UNDYING));
                // 潜影盒中可能没有不死图腾
                if (totemOfUndying.isEmpty()) {
                    continue;
                }
                ItemStack itemStack = totemOfUndying.copy();
                return new Pair<>(itemStack, itemStack.get(DataComponentTypes.DEATH_PROTECTION));
            }
        }
        return null;
    }

    @Unique
    protected void onPlayerBreakBlock() {
        this.getEquipmentChanges();
    }
}