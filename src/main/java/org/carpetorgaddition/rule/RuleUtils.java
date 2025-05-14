package org.carpetorgaddition.rule;

import carpet.api.settings.CarpetRule;
import carpet.utils.Translations;
import net.minecraft.command.PermissionLevelPredicate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.validator.MaxBlockPlaceDistanceValidator;
import org.carpetorgaddition.util.TextUtils;
import org.jetbrains.annotations.Nullable;

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
     * @param supplier  命令权限对应规则的开关
     * @param predicate 原版的权限谓词
     */
    public static PermissionLevelPredicate<ServerCommandSource> requireOrOpenPermissionLevel(
            Supplier<Boolean> supplier,
            PermissionLevelPredicate<ServerCommandSource> predicate
    ) {
        return new PermissionLevelPredicate<>() {
            @Override
            public int requiredLevel() {
                return supplier.get() ? 0 : predicate.requiredLevel();
            }

            @Override
            public boolean test(ServerCommandSource source) {
                return supplier.get() || predicate.test(source);
            }
        };
    }

    /**
     * 获取规则的名称
     */
    public static MutableText simpleTranslationName(CarpetRule<?> rule) {
        String value = String.format("%s.rule.%s.name", rule.settingsManager().identifier(), rule.name());
        if (Translations.hasTranslation(value)) {
            return TextUtils.hoverText(TextUtils.translate(value), rule.name());
        }
        return TextUtils.createText(rule.name());
    }
}
