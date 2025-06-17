package org.carpetorgaddition.debug;

import carpet.api.settings.CarpetRule;
import org.carpetorgaddition.exception.ProductionEnvironmentError;
import org.carpetorgaddition.rule.RuleFactory;

import java.util.function.Supplier;

public class DebugSettings {
    static {
        // 不要在非开发环境加载
        ProductionEnvironmentError.assertDevelopmentEnvironment();
    }

    public static final Supplier<Boolean> openFakePlayerInventory = register(
            RuleFactory.create(Boolean.class, "openFakePlayerInventory", false)
                    .addCategories("Debug")
                    .setDisplayName("打开玩家物品栏")
                    .build()
    );
    public static final Supplier<Boolean> showSoulSandItemCount = register(
            RuleFactory.create(Boolean.class, "showSoulSandItemCount", false)
                    .addCategories("Debug")
                    .setDisplayName("显示灵魂沙上物品的数量")
                    .build()
    );
    public static final Supplier<Boolean> showComparatorLevel = register(
            RuleFactory.create(Boolean.class, "showComparatorLevel", false)
                    .addCategories("Debug")
                    .setDisplayName("显示比较器输出等级")
                    .build()
    );
    public static final Supplier<Boolean> phantomImmediatelySpawn = register(
            RuleFactory.create(Boolean.class, "phantomImmediatelySpawn", false)
                    .addCategories("Debug")
                    .setDisplayName("幻翼立即生成")
                    .build()
    );
    public static final Supplier<Boolean> showBlockBreakingSpeed = register(
            RuleFactory.create(Boolean.class, "showBlockBreakingSpeed", false)
                    .addCategories("Debug")
                    .setDisplayName("显示方块挖掘速度")
                    .build()
    );
    public static final Supplier<Boolean> HUDInformationDisplay = register(
            RuleFactory.create(Boolean.class, "HUDInformationDisplay", false)
                    .addCategories("Debug")
                    .setDisplayName("HUD信息显示")
                    .build()
    );
    public static final Supplier<Boolean> disableExperienceOrbSurround = register(
            RuleFactory.create(Boolean.class, "disableExperienceOrbSurround", false)
                    .addCategories("Debug")
                    .setDisplayName("禁用经验球环绕")
                    .build()
    );

    private static <T> Supplier<T> register(CarpetRule<T> rule) {
        DebugRuleRegistrar.getInstance().extensionSettingsManager().addCarpetRule(rule);
        return rule::value;
    }
}
