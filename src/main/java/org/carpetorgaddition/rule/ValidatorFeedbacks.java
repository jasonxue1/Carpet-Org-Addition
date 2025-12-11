package org.carpetorgaddition.rule;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import net.minecraft.network.chat.Component;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.provider.CommandProvider;

import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("unused")
public class ValidatorFeedbacks {
    private ValidatorFeedbacks() {
    }

    /**
     * 值必须大于%s
     */
    public static Component greaterThan(int number) {
        return TextBuilder.translate("carpet.rule.validate.greater_than", number);
    }

    /**
     * 值必须小于%s
     */
    public static Component lessThan(int number) {
        return TextBuilder.translate("carpet.rule.validate.less_than", number);
    }

    /**
     * 值必须大于等于%s
     */
    public static Component greaterThanOrEqual(int number) {
        return TextBuilder.translate("carpet.rule.validate.greater_than_or_equal", number);
    }

    /**
     * 值必须小于等于%s
     */
    public static Component lessThanOrEqual(int number) {
        return TextBuilder.translate("carpet.rule.validate.less_than_or_equal", number);
    }

    /**
     * 值必须大于等于%s，或者为%s
     */
    public static Component greaterOrEqualOrValue(int number, int other) {
        return TextBuilder.translate("carpet.rule.validate.greater_than_or_equal_or_number", number, other);
    }

    /**
     * 值必须介于%s和%s之间，或者为%s
     */
    public static Component rangeOrValue(int number1, int number2, int other) {
        return TextBuilder.translate("carpet.rule.validate.between_two_number_or_number", number1, number2, other);
    }

    /**
     * 有效选项: [%s1, %s2, ...]
     */
    public static <T> Component validOptions(CarpetRule<T> rule) {
        ArrayList<TextBuilder> list = new ArrayList<>();
        for (String suggestion : rule.suggestions()) {
            TextBuilder option = new TextBuilder(suggestion)
                    .setHover("carpet.settings.command.switch_to", suggestion)
                    .setSuggestCommand(CommandProvider.setCarpetRule(rule.name(), suggestion));
            // 规则默认值设置为粗体
            if (Objects.equals(suggestion, RuleHelper.toRuleString(rule.defaultValue()))) {
                option.setBold();
            }
            // 规则当前值设置为斜体
            if (Objects.equals(suggestion, RuleHelper.toRuleString(rule.value()))) {
                option.setItalic();
            }
            list.add(option);
        }
        Component message = TextBuilder.joinList(list.stream().map(TextBuilder::build).toList(), TextBuilder.create(", "));
        return TextBuilder.translate("carpet.rule.validate.valid_options", message);
    }
}
