package org.carpetorgaddition.mixin.rule;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandManagerMixin {
    @Inject(method = "performCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;executeCommandInContext(Lnet/minecraft/commands/CommandSourceStack;Ljava/util/function/Consumer;)V"))
    private void recordCommand(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.recordPlayerCommand.get()) {
            CommandSourceStack source = parseResults.getContext().getSource();
            CarpetOrgAddition.LOGGER.info("<{}> [Command: /{}]", source.getTextName(), command);
        }
    }
}
