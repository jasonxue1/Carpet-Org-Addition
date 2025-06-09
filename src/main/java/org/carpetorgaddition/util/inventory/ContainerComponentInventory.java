package org.carpetorgaddition.util.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.carpetorgaddition.mixin.accessor.ContainerComponentAccessor;
import org.carpetorgaddition.mixin.accessor.SimpleInventoryAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;

public class ContainerComponentInventory implements Inventory, Iterable<ItemStack> {
    private final ItemStack itemStack;
    private final SimpleInventory inventory;
    public static final int CONTAINER_SIZE = 27;

    public ContainerComponentInventory(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.inventory = new SimpleInventory(CONTAINER_SIZE);
        DefaultedList<ItemStack> list = getItemStacks(itemStack);
        ((SimpleInventoryAccessor) this.inventory).setHeldStacks(list);
    }

    @SuppressWarnings("deprecation")
    private static DefaultedList<ItemStack> getItemStacks(ItemStack itemStack) {
        ContainerComponent component = itemStack.get(DataComponentTypes.CONTAINER);
        if (component == null) {
            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(CONTAINER_SIZE, ItemStack.EMPTY);
            ContainerComponent newComponent = Objects.requireNonNull(ContainerComponent.fromStacks(stacks));
            ContainerComponentAccessor accessor = ContainerComponentAccessor.class.cast(newComponent);
            accessor.setStacks(stacks);
            accessor.setHashCode(ItemStack.listHashCode(stacks));
            itemStack.set(DataComponentTypes.CONTAINER, newComponent);
            return stacks;
        } else {
            ContainerComponentAccessor accessor = ContainerComponentAccessor.class.cast(component);
            DefaultedList<ItemStack> list = accessor.getStacks();
            if (list.size() >= 27) {
                return list;
            }
            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(CONTAINER_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < list.size(); i++) {
                stacks.set(i, list.get(i));
            }
            accessor.setStacks(stacks);
            accessor.setHashCode(ItemStack.listHashCode(stacks));
            return stacks;
        }
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return this.inventory.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return this.inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.setStack(slot, stack);
    }

    @Override
    public void markDirty() {
        this.inventory.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (this.itemStack.isEmpty()) {
            return false;
        }
        return this.itemStack.getCount() == 1 && this.inventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        this.inventory.clear();
    }

    public ItemStack addStack(ItemStack itemStack) {
        return this.inventory.addStack(itemStack);
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
}
