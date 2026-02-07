package boat.carpetorgaddition.mixin.accessor.carpet;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.SettingsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SettingsManager.class)
public interface SettingsManagerAccessor {
    @Invoker("displayInteractiveSetting")
    Component displayInteractiveSettings(CarpetRule<?> rule);

    @SuppressWarnings("UnusedReturnValue")
    @Invoker("setRule")
    int changeRuleValue(CommandSourceStack source, CarpetRule<?> rule, String newValue);
}
