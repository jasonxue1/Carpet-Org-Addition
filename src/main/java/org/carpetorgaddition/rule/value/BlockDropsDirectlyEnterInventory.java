package org.carpetorgaddition.rule.value;

public enum BlockDropsDirectlyEnterInventory {
    TRUE,
    FALSE,
    CUSTOM;

    public boolean isEnable() {
        return this != FALSE;
    }

    public boolean isServerDecision() {
        return this != CUSTOM;
    }
}
