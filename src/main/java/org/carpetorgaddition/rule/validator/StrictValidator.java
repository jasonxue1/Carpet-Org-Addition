package org.carpetorgaddition.rule.validator;

import carpet.api.settings.RuleHelper;
import net.minecraft.text.Text;
import org.carpetorgaddition.rule.RuleValidatorProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class StrictValidator<T> extends AbstractValidator<T> {
    private final String rule;
    private final Collection<String> suggestions;

    public StrictValidator(String rule, Collection<String> suggestions) {
        this.rule = rule;
        this.suggestions = suggestions;
    }

    @Override
    public boolean validate(T newValue) {
        return this.suggestions.contains(RuleHelper.toRuleString(newValue));
    }

    @Override
    public @NotNull Text errorMessage() {
        return RuleValidatorProvider.validOptions(this.rule, this.suggestions);
    }
}
