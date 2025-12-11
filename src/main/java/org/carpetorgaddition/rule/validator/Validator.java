package org.carpetorgaddition.rule.validator;

import carpet.api.settings.CarpetRule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Validator<T> {
    /**
     * 规则的新值是否有效
     *
     * @param newValue 规则的新值
     * @return 如果值有效，返回true，否则返回false
     * @apiNote 与此方法重载方法一样不得抛出异常
     */
    boolean validate(T newValue);

    /**
     * @return 规则校验失败时的错误反馈
     */
    @NotNull
    Component errorMessage();


    default void notifyFailure(CommandSourceStack source, CarpetRule<T> currentRule, String providedValue) {
        // 获取此规则的翻译名称
        Component name = RuleUtils.simpleTranslationName(currentRule);
        MessageUtils.sendErrorMessage(source, "carpet.rule.validate.invalid_value", name, providedValue);
        MessageUtils.sendErrorMessage(source, errorMessage());
    }

    static <T> Validator<T> of(Predicate<T> predicate, Supplier<Component> supplier) {
        return new Validator<>() {
            @Override
            public boolean validate(T newValue) {
                return predicate.test(newValue);
            }

            @Override
            public @NotNull Component errorMessage() {
                return supplier.get();
            }
        };
    }
}
