package org.carpetorgaddition.rule.validator;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import net.minecraft.text.Text;
import org.carpetorgaddition.rule.RuleValidatorProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class StrictValidator<T> extends AbstractValidator<T> {
    private final CarpetRule<T> rule;

    public StrictValidator(CarpetRule<T> rule) {
        this.rule = rule;
    }

    @Override
    public boolean validate(T newValue) {
        return this.rule.suggestions().contains(RuleHelper.toRuleString(newValue));
    }

    @Override
    public @NotNull Text errorMessage() {
        return RuleValidatorProvider.validOptions(this.rule);
    }
}
