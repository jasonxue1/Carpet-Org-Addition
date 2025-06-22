package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.command.PermissionLevelPredicate;
import net.minecraft.server.command.SeedCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SeedCommand.class)
public class SeedCommandMixin {
    // 开放/seed权限
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/command/CommandManager;requirePermissionLevel(I)Lnet/minecraft/command/PermissionLevelPredicate;"))
    private static PermissionLevelPredicate<ServerCommandSource> privilege(int requiredLevel, Operation<PermissionLevelPredicate<ServerCommandSource>> original) {
        return RuleUtils.requireOrOpenPermissionLevel(() -> CarpetOrgAdditionSettings.openSeedPermissions.get(), original.call(requiredLevel));
    }
}
