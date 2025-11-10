package org.carpetorgaddition.rule;

import org.carpetorgaddition.CarpetOrgAdditionSettings;

public class RuleSelfConstants {
    public static final String blockDropsDirectlyEnterInventory;
    public static final String itemPickupRangeExpand;

    static {
        try {
            blockDropsDirectlyEnterInventory = CarpetOrgAdditionSettings.class.getField("blockDropsDirectlyEnterInventory").getName();
            itemPickupRangeExpand = CarpetOrgAdditionSettings.class.getField("itemPickupRangeExpand").getName();
        } catch (NoSuchFieldException e) {
            // 不处理，直接抛出
            throw new RuntimeException(e);
        }
    }
}
