package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.world.Container;

public interface PlayerInventoryDecomposer {
    /**
     * 获取玩家物品栏主要存储部分
     */
    Container getStorage();

    /**
     * 获取玩家物品栏快捷栏部分
     */
    Container getHotbar();

    /**
     * 获取玩家物品栏盔甲部分
     */
    Container getArmor();

    /**
     * 获取玩家物品栏副手部分
     */
    Container getOffHand();
}
