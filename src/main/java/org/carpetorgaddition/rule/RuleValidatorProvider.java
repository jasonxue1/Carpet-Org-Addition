package org.carpetorgaddition.rule;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.provider.CommandProvider;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("unused")
public class RuleValidatorProvider {
    private RuleValidatorProvider() {
    }

    /**
     * 值必须大于%s
     */
    public static Text greaterThan(int number) {
        return TextBuilder.translate("carpet.rule.validate.greater_than", number);
    }

    /**
     * 值必须小于%s
     */
    public static Text lessThan(int number) {
        return TextBuilder.translate("carpet.rule.validate.less_than", number);
    }

    /**
     * 值必须大于等于%s
     */
    public static Text greaterThanOrEqual(int number) {
        return TextBuilder.translate("carpet.rule.validate.greater_than_or_equal", number);
    }

    /**
     * 值必须小于等于%s
     */
    public static Text lessThanOrEqual(int number) {
        return TextBuilder.translate("carpet.rule.validate.less_than_or_equal", number);
    }

    /**
     * 值必须大于等于%s，或者为%s
     */
    public static Text greaterThanOrEqualOrNumber(int number, int other) {
        return TextBuilder.translate("carpet.rule.validate.greater_than_or_equal_or_number", number, other);
    }

    /**
     * 值必须介于%s和%s之间，或者为%s
     */
    public static Text betweenTwoNumberOrNumber(int number1, int number2, int other) {
        return TextBuilder.translate("carpet.rule.validate.between_two_number_or_number", number1, number2, other);
    }

    /**
     * 无效值: {@code value}
     */
    public static Text validOptions(String rule, Collection<String> suggestions) {
        ArrayList<TextBuilder> list = new ArrayList<>();
        for (String suggestion : suggestions) {
            TextBuilder option = new TextBuilder(suggestion)
                    .setColor(Formatting.AQUA)
                    .setHover("carpet.settings.command.switch_to", suggestion)
                    .setCommand(CommandProvider.setCarpetRule(rule, suggestion));
            list.add(option);
        }
        TextBuilder builder = new TextBuilder();
        int len = list.size();
        for (int i = 0; i < len; i++) {
            builder.append(list.get(i));
            if (i < len - 1) {
                builder.append(", ");
            }
        }
        return TextBuilder.translate("carpet.rule.validate.valid_options", builder.build());
    }
}
