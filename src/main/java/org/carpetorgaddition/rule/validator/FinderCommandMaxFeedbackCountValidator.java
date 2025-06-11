package org.carpetorgaddition.rule.validator;

import net.minecraft.text.Text;
import org.carpetorgaddition.wheel.provider.RuleValidatorProvider;
import org.jetbrains.annotations.NotNull;

public class FinderCommandMaxFeedbackCountValidator extends AbstractValidator<Integer> {
    @Override
    public boolean validate(Integer newValue) {
        return newValue > 0;
    }

    @Override
    public @NotNull Text errorMessage() {
        return RuleValidatorProvider.greaterThan(0);
    }
}
