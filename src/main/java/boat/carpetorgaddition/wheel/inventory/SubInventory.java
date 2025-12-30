package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

/**
 * 物品栏的子物品栏，对子物品栏的所有操作都会反映在原本的物品栏中
 */
@NullMarked
public class SubInventory implements Container {
    private final Container container;
    private final int start;
    private final int end;

    /**
     * @param container 原本的物品栏
     * @param start     物品栏的起始索引（包含）
     * @param end       物品栏的结束索引（不包含）
     */
    public SubInventory(Container container, int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("start(%d) > end(%d)".formatted(start, end));
        }
        this.container = container;
        this.start = start;
        this.end = end;
    }

    @Override
    public int getContainerSize() {
        return this.end - this.start;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.container.getItem(this.start + slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return this.container.removeItem(this.start + slot, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.container.removeItemNoUpdate(this.start + slot);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.container.setItem(this.start + slot, itemStack);
    }

    @Override
    public void setChanged() {
        this.container.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.removeItemNoUpdate(i);
        }
    }
}
