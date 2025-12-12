package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 不可变的物品栏，一旦创建，里面的内容都是不可以改变的，只能进行查询操作，否则抛出{@link UnsupportedOperationException}
 */
public final class ImmutableInventory implements Container, Iterable<ItemStack> {
    /**
     * 空物品栏
     */
    public static final ImmutableInventory EMPTY = new ImmutableInventory();

    private final SimpleContainer inventory;

    public ImmutableInventory(List<ItemStack> list) {
        this.inventory = new SimpleContainer(list.toArray(ItemStack[]::new));
    }

    public ImmutableInventory(Container inventory) {
        ArrayList<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            list.add(inventory.getItem(i));
        }
        this.inventory = new SimpleContainer(list.toArray(ItemStack[]::new));
    }

    public ImmutableInventory(ItemStack itemStack) {
        this(List.of(itemStack));
    }

    private ImmutableInventory() {
        this.inventory = new SimpleContainer();
    }

    @Override
    public int getContainerSize() {
        return this.inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this == EMPTY || this.inventory.isEmpty();
    }

    @Override
    public @NonNull ItemStack getItem(int slot) {
        return this.inventory.getItem(slot);
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int amount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setChanged() {
        this.inventory.setChanged();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", this.getClass().getSimpleName() + ":{", "}");
        for (int index = 0; index < this.getContainerSize(); index++) {
            ItemStack itemStack = this.getItem(index);
            if (itemStack.isEmpty()) {
                continue;
            }
            joiner.add(itemStack.getItem() + "*" + itemStack.getCount());
        }
        return joiner.toString();
    }

    @NotNull
    @Override
    public java.util.Iterator<ItemStack> iterator() {
        return new java.util.Iterator<>() {
            // 要返回的下一个元素的索引
            private int cursor = 0;

            // 迭代器的大小
            private final int size = ImmutableInventory.this.getContainerSize();

            @Override
            public boolean hasNext() {
                return this.cursor < this.size;
            }

            @Override
            public ItemStack next() {
                // 由于对象不可变，所以是线程安全的，不需要考虑并发修改的问题
                ItemStack itemStack = ImmutableInventory.this.getItem(cursor);
                this.cursor++;
                return itemStack;
            }
        };
    }
}
