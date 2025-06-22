package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.command.PermissionLevelPredicate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {
    // 开放/tp命令权限
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;requirePermissionLevel(I)Lnet/minecraft/command/PermissionLevelPredicate;", ordinal = 0))
    private static PermissionLevelPredicate<ServerCommandSource> tpPermissions(int requiredLevel, Operation<PermissionLevelPredicate<ServerCommandSource>> original) {
        return RuleUtils.requireOrOpenPermissionLevel(() -> CarpetOrgAdditionSettings.openTpPermissions.get(), original.call(requiredLevel));
    }

    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;requirePermissionLevel(I)Lnet/minecraft/command/PermissionLevelPredicate;", ordinal = 1))
    private static PermissionLevelPredicate<ServerCommandSource> teleportPermissions(int requiredLevel, Operation<PermissionLevelPredicate<ServerCommandSource>> original) {
        return RuleUtils.requireOrOpenPermissionLevel(() -> CarpetOrgAdditionSettings.openTpPermissions.get(), original.call(requiredLevel));
    }
}
