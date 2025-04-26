package org.carpetorgaddition.rule;

import org.carpetorgaddition.CarpetOrgAdditionSettings;

public class RuleSelfConstants {
    public static final String blockDropsDirectlyEnterInventory;

    static {
        try {
            blockDropsDirectlyEnterInventory = CarpetOrgAdditionSettings.class.getField("blockDropsDirectlyEnterInventory").getName();
        } catch (NoSuchFieldException e) {
            // 不处理，直接抛出
            throw new RuntimeException(e);
        }
    }
}
