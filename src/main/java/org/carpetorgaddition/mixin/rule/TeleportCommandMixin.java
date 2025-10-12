package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionSource;
import net.minecraft.command.permission.PermissionSourcePredicate;
import net.minecraft.server.command.TeleportCommand;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {
    // 开放/tp命令权限
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;requirePermissionLevel(Lnet/minecraft/command/permission/PermissionCheck;)Lnet/minecraft/command/permission/PermissionSourcePredicate;", ordinal = 0))
    private static <T extends PermissionSource> PermissionSourcePredicate<T> tpPermissions(PermissionCheck permissionCheck, Operation<PermissionSourcePredicate<T>> original) {
        return RuleUtils.requireOrOpenPermissionLevel(CarpetOrgAdditionSettings.openTpPermission, original.call(permissionCheck));
    }

    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;requirePermissionLevel(Lnet/minecraft/command/permission/PermissionCheck;)Lnet/minecraft/command/permission/PermissionSourcePredicate;", ordinal = 1))
    private static <T extends PermissionSource> PermissionSourcePredicate<T> teleportPermissions(PermissionCheck permissionCheck, Operation<PermissionSourcePredicate<T>> original) {
        return RuleUtils.requireOrOpenPermissionLevel(CarpetOrgAdditionSettings.openTpPermission, original.call(permissionCheck));
    }
}
