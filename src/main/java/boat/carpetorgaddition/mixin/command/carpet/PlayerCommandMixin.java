package boat.carpetorgaddition.mixin.command.carpet;

import boat.carpetorgaddition.command.PlayerCommandExtension;
import carpet.commands.PlayerCommand;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerCommand.class)
public class PlayerCommandMixin {
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;suggests(Lcom/mojang/brigadier/suggestion/SuggestionProvider;)Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;", remap = false))
    private static RequiredArgumentBuilder<CommandSourceStack, ?> register(RequiredArgumentBuilder<CommandSourceStack, ?> instance, SuggestionProvider<CommandSourceStack> provider, Operation<RequiredArgumentBuilder<CommandSourceStack, ?>> original) {
        RequiredArgumentBuilder<CommandSourceStack, ?> call = original.call(instance, provider);
        return PlayerCommandExtension.register(call);
    }
}
