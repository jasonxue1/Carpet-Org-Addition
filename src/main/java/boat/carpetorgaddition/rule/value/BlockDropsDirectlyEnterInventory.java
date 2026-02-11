package boat.carpetorgaddition.rule.value;

public enum BlockDropsDirectlyEnterInventory {
    TRUE,
    FALSE,
    CUSTOM;

    public boolean isEnabled() {
        return this == TRUE;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }

    public static BlockDropsDirectlyEnterInventory active(boolean enabled) {
        return enabled ? TRUE : FALSE;
    }
}
