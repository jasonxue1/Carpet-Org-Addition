package org.carpetorgaddition.wheel.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.StringJoiner;
import java.util.function.Predicate;

public class ContainerComponentInventory extends SimpleInventory {
    private final ItemStack itemStack;
    public static final int CONTAINER_SIZE = 27;

    public ContainerComponentInventory(ItemStack itemStack) {
        super(CONTAINER_SIZE);
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
}
