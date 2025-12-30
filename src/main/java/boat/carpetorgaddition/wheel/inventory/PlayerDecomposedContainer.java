package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.world.Container;

public interface PlayerDecomposedContainer extends Container {
    Container getPlayerInventory();

    /**
     * 获取玩家物品栏主要存储部分
     */
    default Container getStorage() {
        return new SubInventory(this.getPlayerInventory(), 9, 36);
    }

    /**
     * 获取玩家物品栏快捷栏部分
     */
    default Container getHotbar() {
        return new SubInventory(this.getPlayerInventory(), 0, 9);
    }

    /**
     * 获取玩家物品栏盔甲部分
     */
    default Container getArmor() {
        return new ReverseInventroy(new SubInventory(this.getPlayerInventory(), 36, 40));
    }

    /**
     * 获取玩家物品栏副手部分
     */
    default Container getOffHand() {
        return new SubInventory(this.getPlayerInventory(), 40, 41);
    }
}
