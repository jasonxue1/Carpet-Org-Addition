package org.carpetorgaddition.rule.validator;

import net.minecraft.text.Text;
import org.carpetorgaddition.wheel.provider.RuleValidatorProvider;
import org.jetbrains.annotations.NotNull;

// 自定义猪灵交易时间
public class PiglinBarteringTimeValidator extends AbstractValidator<Long> {
    @Override
    public boolean validate(Long newValue) {
        return newValue >= 0 || newValue == -1;
    }

    @Override
    public @NotNull Text errorMessage() {
        return RuleValidatorProvider.greaterThanOrEqualOrNumber(0, -1);
    }
}
