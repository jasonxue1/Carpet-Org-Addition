package org.carpetorgaddition.rule.value;

public enum BetterTotemOfUndying {
    /**
     * 启用后可以使用物品栏中的不死图腾，但不会使用物品栏中潜影盒里的不死图腾
     */
    INVENTORY,
    /**
     * 原版行为
     */
    VANILLA,
    /**
     * 启用后可以使用物品栏中的不死图腾，包括物品栏中潜影盒里的
     */
    INVENTORY_WITH_SHULKER_BOX
}
