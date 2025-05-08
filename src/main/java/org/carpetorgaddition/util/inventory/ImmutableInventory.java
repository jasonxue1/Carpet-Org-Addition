package org.carpetorgaddition.util.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 不可变的物品栏，一旦创建，里面的内容都是不可以改变的，只能进行查询操作，否则抛出{@link UnsupportedOperationException}
 */
public final class ImmutableInventory implements Inventory, Iterable<ItemStack> {
    /**
     * 空物品栏
     */
    public static final ImmutableInventory EMPTY = new ImmutableInventory();

    private final SimpleInventory inventory;

    public ImmutableInventory(List<ItemStack> list) {
        this.inventory = new SimpleInventory(list.toArray(ItemStack[]::new));
    }

    public ImmutableInventory(Inventory inventory) {
        ArrayList<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            list.add(inventory.getStack(i));
        }
        this.inventory = new SimpleInventory(list.toArray(ItemStack[]::new));
    }

    private ImmutableInventory() {
        this.inventory = new SimpleInventory();
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this == EMPTY || this.inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemStack removeStack(int slot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markDirty() {
        this.inventory.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", this.getClass().getSimpleName() + ":{", "}");
        for (int index = 0; index < this.size(); index++) {
            ItemStack itemStack = this.getStack(index);
            if (itemStack.isEmpty()) {
                continue;
            }
            joiner.add(itemStack.getItem().toString() + "*" + itemStack.getCount());
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
            private final int size = ImmutableInventory.this.size();

            @Override
            public boolean hasNext() {
                return this.cursor < this.size;
            }

            @Override
            public ItemStack next() {
                // 由于对象不可变，所以是线程安全的，不需要考虑并发修改的问题
                ItemStack itemStack = ImmutableInventory.this.getStack(cursor);
                this.cursor++;
                return itemStack;
            }
        };
    }
}
