package org.carpetorgaddition.rule;

import carpet.api.settings.CarpetRule;
import carpet.utils.TranslationKeys;
import carpet.utils.Translations;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.validator.MaxBlockPlaceDistanceValidator;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class RuleUtils {

    /**
     * 潜影盒是否可以触发更新抑制器
     */
    public static boolean canUpdateSuppression(@Nullable String blockName) {
        if ("false".equalsIgnoreCase(CarpetOrgAdditionSettings.CCEUpdateSuppression)) {
            return false;
        }
        if (blockName == null) {
            return false;
        }
        if ("true".equalsIgnoreCase(CarpetOrgAdditionSettings.CCEUpdateSuppression)) {
            return "更新抑制器".equals(blockName) || "updateSuppression".equalsIgnoreCase(blockName);
        }
        // 比较字符串并忽略大小写
        return Objects.equals(CarpetOrgAdditionSettings.CCEUpdateSuppression.toLowerCase(), blockName.toLowerCase());
    }

    public static boolean isDefaultDistance() {
        return CarpetOrgAdditionSettings.maxBlockPlaceDistance == -1;
    }

    /**
     * 获取Carpet Org Addition设置的玩家最大交互距离并进行判断，小于0的值会被视为6.0，超过256的值会被视为256.0
     *
     * @return 当前设置的最大交互距离，最大不超过256.0
     */
    public static double getPlayerMaxInteractionDistance() {
        double distance = CarpetOrgAdditionSettings.maxBlockPlaceDistance;
        if (distance < 0) {
            return 6.0;
        }
        return Math.min(distance, MaxBlockPlaceDistanceValidator.MAX_VALUE);
    }

    public static <T> T shulkerBoxStackableWrap(Supplier<T> supplier) {
        boolean changed = CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.get();
        try {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(false);
            return supplier.get();
        } finally {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(changed);
        }
    }

    /**
     * @return 规则方块掉落物直接进入物品栏是否可用
     */
    public static boolean canCollectBlock(@Nullable ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        return switch (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory) {
            case TRUE -> true;
            case FALSE -> false;
            case CUSTOM -> {
                RuleSelfManager ruleSelfManager = FetcherUtils.getRuleSelfManager(player);
                yield ruleSelfManager.isEnabled(player, RuleSelfConstants.blockDropsDirectlyEnterInventory);
            }
        };
    }

    /**
     * 获取规则的名称
     */
    public static MutableText simpleTranslationName(CarpetRule<?> rule) {
        String key = String.format(TranslationKeys.RULE_NAME_PATTERN, rule.settingsManager().identifier(), rule.name());
        TextBuilder builder = TextBuilder.of(key);
        if (Translations.hasTranslation(key)) {
            return builder.setHover(rule.name()).build();
        }
        return TextBuilder.create(rule.name());
    }

    public static List<Text> ruleExtraInfo(CarpetRule<?> rule) {
        String key = String.format(TranslationKeys.RULE_EXTRA_PREFIX_PATTERN, rule.settingsManager().identifier(), rule.name());
        List<String> list = new ArrayList<>();
        for (int i = 0; Translations.hasTranslation(key + i); i++) {
            list.add(Translations.tr(key + i));
        }
        return list.stream()
                .map(TextBuilder::of)
                .map(builder -> builder.setColor(Formatting.GRAY))
                .map(TextBuilder::build)
                .map(text -> (Text) text)
                .toList();
    }
}
