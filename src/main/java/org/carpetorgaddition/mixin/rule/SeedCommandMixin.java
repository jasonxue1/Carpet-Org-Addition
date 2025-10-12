package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.command.permission.PermissionCheck;
import net.minecraft.command.permission.PermissionSource;
import net.minecraft.command.permission.PermissionSourcePredicate;
import net.minecraft.server.command.SeedCommand;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SeedCommand.class)
public class SeedCommandMixin {
    // 开放/seed权限
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;requirePermissionLevel(Lnet/minecraft/command/permission/PermissionCheck;)Lnet/minecraft/command/permission/PermissionSourcePredicate;"))
    private static <T extends PermissionSource> PermissionSourcePredicate<T> privilege(PermissionCheck permissionCheck, Operation<PermissionSourcePredicate<T>> original) {
        return RuleUtils.requireOrOpenPermissionLevel(CarpetOrgAdditionSettings.openSeedPermission, original.call(permissionCheck));
    }
}
