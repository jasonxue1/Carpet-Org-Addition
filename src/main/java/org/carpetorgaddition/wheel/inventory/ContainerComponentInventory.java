package org.carpetorgaddition.wheel.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.wheel.ItemStackPredicate;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;

public class ContainerComponentInventory extends SimpleInventory implements Iterable<ItemStack>, Comparable<ContainerComponentInventory> {
    private final ItemStack itemStack;
    public static final int CONTAINER_SIZE = 27;

    public ContainerComponentInventory(ItemStack itemStack) {
        super(CONTAINER_SIZE);
        InventoryUtils.deepCopyContainer(itemStack);
        this.itemStack = itemStack;
        ContainerComponent component = this.itemStack.get(DataComponentTypes.CONTAINER);
        if (component != null) {
            component.copyTo(this.getHeldStacks());
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        DefaultedList<ItemStack> stacks = this.getHeldStacks();
        this.itemStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.itemStack.isEmpty()) {
            return false;
        }
        return this.itemStack.getCount() == 1 && super.canPlayerUse(player);
    }

    /**
     * @return 从潜影盒中获取指定物品
     */
    @CheckReturnValue
    public ItemStack pinkStack(Predicate<ItemStack> predicate) {
        for (int i = 0; i < this.size(); i++) {
            ItemStack stack = this.getStack(i);
            if (predicate.test(stack)) {
                ItemStack result = stack.copy();
                this.setStack(i, ItemStack.EMPTY);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * @return 从潜影盒中获取指定数量的物品
     */
    @CheckReturnValue
    public ItemStack pinkStack(Predicate<ItemStack> predicate, int count) {
        for (int i = 0; i < this.size(); i++) {
            ItemStack stack = this.getStack(i);
            if (predicate.test(stack)) {
                return this.removeStack(i, count);
            }
        }
        return ItemStack.EMPTY;
    }

    public int count(Predicate<ItemStack> predicate) {
        int count = 0;
        for (ItemStack stack : this) {
            if (stack.isEmpty()) {
                continue;
            }
            if (predicate.test(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContainerComponentInventory inventory = (ContainerComponentInventory) o;
        return InventoryUtils.canMerge(itemStack, inventory.itemStack);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashCode(this.itemStack);
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "{Size: %s[]}".formatted(this.size());
        }
        StringJoiner joiner = new StringJoiner(", ", "{Size: %s[".formatted(this.size()), "]}");
        for (ItemStack itemStack : this) {
            if (itemStack.isEmpty()) {
                continue;
            }
            joiner.add(itemStack.getItem().toString() + "*" + itemStack.getCount());
        }
        return joiner.toString();
    }

    @Override
    public @NotNull Iterator<ItemStack> iterator() {
        return new Iterator<>() {
            private int cursor = 0;
            private final int size = size();

            @Override
            public boolean hasNext() {
                return this.cursor < this.size;
            }

            @Override
            public ItemStack next() {
                ItemStack itemStack = getStack(cursor);
                this.cursor++;
                return itemStack;
            }
        };
    }

    @Override
    public int compareTo(@NotNull ContainerComponentInventory o) {
        // 容器内物品相同
        if (this.equals(o)) {
            return 0;
        }
        // 有物品的潜影盒放在空潜影盒前面
        boolean thisIsEmpty = this.isEmpty();
        boolean otherIsEmpty = o.isEmpty();
        // 两个物品栏均为空
        if (thisIsEmpty && otherIsEmpty) {
            return 0;
        }
        if (thisIsEmpty) {
            return 1;
        }
        if (otherIsEmpty) {
            return -1;
        }
        Optional<Item> thisOptional = this.singleItemType();
        Optional<Item> otherOptional = o.singleItemType();
        // 只有一种物品的潜影盒排在前面
        if (thisOptional.isEmpty() && otherOptional.isEmpty()) {
            // 两个潜影盒都包含多种物品，按物品数量和哈希值排序
            int compareToCount = compareToCount(o);
            if (compareToCount != 0) {
                return compareToCount;
            }
            return Integer.compare(this.hashCode(), o.hashCode());
        }
        if (thisOptional.isEmpty()) {
            return 1;
        }
        if (otherOptional.isEmpty()) {
            return -1;
        }
        // 两个物品栏内均只有一种物品，按物品ID排序
        int compareId = InventoryUtils.compare(thisOptional.get(), otherOptional.get());
        if (compareId != 0) {
            return compareId;
        }
        // 如果物品ID相同，按照物品数量排序
        int compareCount = compareToCount(o);
        if (compareCount != 0) {
            return compareCount;
        }
        return Integer.compare(this.hashCode(), o.hashCode());
    }

    private int compareToCount(@NotNull ContainerComponentInventory o) {
        int thisCount = InventoryUtils.count(this, ItemStackPredicate.WILDCARD);
        int otherCount = InventoryUtils.count(o, ItemStackPredicate.WILDCARD);
        return -Integer.compare(thisCount, otherCount);
    }

    /**
     * @return 如果容器中只有一种物品，返回物品的类型，否则返回空
     */
    private Optional<Item> singleItemType() {
        Item item = null;
        for (ItemStack stack : this) {
            if (stack.isEmpty()) {
                continue;
            }
            if (item == null) {
                item = stack.getItem();
            }
            if (item == stack.getItem()) {
                continue;
            }
            return Optional.empty();
        }
        return Optional.ofNullable(item);
    }
}
