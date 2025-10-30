package org.carpetorgaddition.mixin.rule.fakeplayerkeepinventory;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.dedicated.management.dispatch.GameRuleType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRules;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Unique
    private final ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;

    @SuppressWarnings("unchecked")
    @WrapOperation(method = "copyFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/rule/GameRules;getValue(Lnet/minecraft/world/rule/GameRule;)Ljava/lang/Object;"))
    private <T> T keepItem(GameRules instance, GameRule<T> rule, Operation<T> original) {
        if (this.shouldKeepInventory() && rule.getType() == GameRuleType.BOOL) {
            return (T) Boolean.TRUE;
        }
        return original.call(instance, rule);
    }

    @Unique
    private boolean shouldKeepInventory() {
        if (CarpetOrgAdditionSettings.fakePlayerKeepInventory.get() && thisPlayer instanceof EntityPlayerMPFake fakePlayer) {
            return RuleUtils.shouldKeepInventory(fakePlayer);
        }
        return false;
    }
}
