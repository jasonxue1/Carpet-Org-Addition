package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.CarpetOrgAdditionExtension;
import boat.carpetorgaddition.mixin.accessor.carpet.SettingsManagerAccessor;
import carpet.api.settings.CarpetRule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class RuleAccessor<T> {
    private final Supplier<T> value;
    private final CarpetRule<T> rule;
    private final String key;

    public RuleAccessor(RuleContext<T> context) {
        this.value = () -> (CarpetOrgAdditionExtension.isCarpetRuleLoaded() ? context.rule().value() : context.value());
        this.rule = context.rule();
        this.key = this.rule.name();
    }

    public T value() {
        return this.value.get();
    }

    public CarpetRule<T> getCarpetRule() {
        return this.rule;
    }

    public Component getDisplayName() {
        return RuleUtils.simpleTranslationName(this.rule);
    }

    public String getKey() {
        return this.key;
    }

    // TODO 不必要的rule参数
    public void setRuleValue(CommandSourceStack source, CarpetRule<?> rule, String newValue) {
        SettingsManagerAccessor accessor = (SettingsManagerAccessor) CarpetOrgAdditionExtension.getSettingManager();
        accessor.changeRuleValue(source, rule, newValue);
    }
}
