package org.carpetorgaddition.rule.value;

public enum BlockDropsDirectlyEnterInventory {
    TRUE,
    FALSE,
    CUSTOM;

    /**
     * 是否不允许玩家自行决定是否启用规则
     */
    public boolean isServerDecision() {
        return this != CUSTOM;
    }
}
