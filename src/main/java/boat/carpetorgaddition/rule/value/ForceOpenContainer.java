package boat.carpetorgaddition.rule.value;

public enum ForceOpenContainer {
    /**
     * 不能打开被阻挡的容器
     */
    FALSE,
    /**
     * 可以打开被阻挡的潜影盒
     */
    SHULKER_BOX,
    /**
     * 可以打开被阻挡的潜影盒，箱子，末影箱
     */
    ANY;

    public boolean canOpenShulkerBox() {
        return this == SHULKER_BOX || this == ANY;
    }

    public boolean canOpenChest() {
        return this == ANY;
    }
}
