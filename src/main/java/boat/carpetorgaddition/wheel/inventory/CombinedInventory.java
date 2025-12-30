package boat.carpetorgaddition.wheel.inventory;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 将多个物品栏合并成一个大物品栏
 */
public class CombinedInventory implements Container {
    private final Int2ObjectMap<Int2ObjectMap.Entry<Container>> indexToContainer = new Int2ObjectArrayMap<>();
    private final Set<Container> containers = new HashSet<>();
    private final int size;

    public CombinedInventory(List<Container> containers) {
        int total = 0;
        for (Container container : containers) {
            this.containers.add(container);
            int size = container.getContainerSize();
            // 键是子物品栏的起始索引，值是物品栏
            Int2ObjectMap.Entry<Container> entry = Int2ObjectMap.entry(total, container);
            for (int i = 0; i < size; i++) {
                this.indexToContainer.put(total + i, entry);
            }
            total += size;
        }
        this.size = total;
    }

    @Override
    public int getContainerSize() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.containers.stream().allMatch(Container::isEmpty);
    }

    @NullMarked
    @Override
    public ItemStack getItem(int slot) {
        Int2ObjectMap.Entry<Container> entry = this.indexToContainer.get(slot);
        int start = entry.getIntKey();
        Container container = entry.getValue();
        return container.getItem(slot - start);
    }

    @NullMarked
    @Override
    public ItemStack removeItem(int slot, int count) {
        Int2ObjectMap.Entry<Container> entry = this.indexToContainer.get(slot);
        int start = entry.getIntKey();
        Container container = entry.getValue();
        return container.removeItem(slot - start, count);
    }

    @NullMarked
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        Int2ObjectMap.Entry<Container> entry = this.indexToContainer.get(slot);
        int start = entry.getIntKey();
        Container container = entry.getValue();
        return container.removeItemNoUpdate(slot - start);
    }

    @NullMarked
    @Override
    public void setItem(int slot, ItemStack itemStack) {
        Int2ObjectMap.Entry<Container> entry = this.indexToContainer.get(slot);
        int start = entry.getIntKey();
        Container container = entry.getValue();
        container.setItem(slot - start, itemStack);
    }

    @Override
    public void setChanged() {
        this.containers.forEach(Container::setChanged);
    }

    @NullMarked
    @Override
    public boolean stillValid(Player player) {
        return this.containers.stream().allMatch(container -> container.stillValid(player));
    }

    @Override
    public void clearContent() {
        this.containers.forEach(Container::clearContent);
    }

    @Nullable
    public Container getSubInventory(int index) {
        Int2ObjectMap.Entry<Container> entry = this.indexToContainer.get(index);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }
}
