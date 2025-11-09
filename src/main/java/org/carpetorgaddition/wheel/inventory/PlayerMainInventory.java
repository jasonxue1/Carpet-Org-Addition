package org.carpetorgaddition.wheel.inventory;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 一个不包括盔甲槽的玩家物品栏
 */
public class PlayerMainInventory implements Inventory {
    private final PlayerInventory playerInventory;
    private final EntityPlayerMPFake fakePlayer;
    private final List<Map.Entry<Integer, DefaultedList<ItemStack>>> combine;

    public PlayerMainInventory(EntityPlayerMPFake fakePlayer) {
        this.playerInventory = fakePlayer.getInventory();
        this.fakePlayer = fakePlayer;
        ArrayList<Map.Entry<Integer, DefaultedList<ItemStack>>> list = new ArrayList<>();
        DefaultedList<ItemStack> main = this.playerInventory.main;
        list.add(Map.entry(0, main));
        DefaultedList<ItemStack> armor = this.playerInventory.armor;
        DefaultedList<ItemStack> offHand = this.playerInventory.offHand;
        list.add(Map.entry(main.size() + armor.size(), offHand));
        this.combine = list;
    }

    @Override
    public int size() {
        int size = 0;
        for (Map.Entry<Integer, DefaultedList<ItemStack>> entry : this.combine) {
            DefaultedList<ItemStack> stacks = entry.getValue();
            size += stacks.size();
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (Map.Entry<Integer, DefaultedList<ItemStack>> entry : this.combine) {
            DefaultedList<ItemStack> stacks = entry.getValue();
            if (stacks.isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.playerInventory.getStack(this.map(slot));
    }

    public ItemStack getStack(Hand hand) {
        return switch (hand) {
            case MAIN_HAND -> this.fakePlayer.getMainHandStack();
            case OFF_HAND -> this.fakePlayer.getOffHandStack();
        };
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return this.playerInventory.removeStack(this.map(slot), amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return this.playerInventory.removeStack(this.map(slot));
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.playerInventory.setStack(this.map(slot), stack);
    }

    @Override
    public void markDirty() {
        this.playerInventory.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.playerInventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.size(); i++) {
            this.setStack(i, ItemStack.EMPTY);
        }
    }

    /**
     * 将物品放入玩家空槽位，如果没有空槽位，则插入到可以接收物品的潜影盒中，如果依然没有空槽位，则丢弃物品。
     */
    public void insertOrDropStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        itemStack = itemStack.copyAndEmpty();
        PlayerInventory inventory = this.fakePlayer.getInventory();
        inventory.insertStack(itemStack);
        if (itemStack.isEmpty()) {
            return;
        }
        itemStack = InventoryUtils.putItemToInventoryShulkerBox(itemStack, this.fakePlayer);
        if (itemStack.isEmpty()) {
            return;
        }
        this.fakePlayer.dropItem(itemStack, false, false);
    }

    /**
     * 整理物品栏
     */
    public void sort() {
        // 记录所有未被锁定的槽位
        ArrayList<Integer> list = new ArrayList<>();
        // 合并相同的物品
        for (int index = 0; index < this.size(); index++) {
            if (this.isValidSlot(index)) {
                list.add(index);
                ItemStack itemStack = this.getStack(index);
                // 物品不可堆叠或堆叠已满
                if (InventoryUtils.isItemStackFull(itemStack)) {
                    continue;
                }
                this.merge(index, itemStack);
            }
        }
        // 整理物品
        sort(list);
    }

    private void sort(List<Integer> list) {
        if (list.isEmpty()) {
            return;
        }
        int start = 0;
        int end = list.size() - 1;
        // 基准物品
        ItemStack pivot = this.getStack(list.getFirst());
        while (start < end) {
            // 程序是否陷入了死循环
            boolean infiniteLoop = true;
            while (end > start && InventoryUtils.compare(pivot, this.getStack(list.get(end))) <= 0) {
                end--;
                infiniteLoop = false;
            }
            while (end > start && InventoryUtils.compare(pivot, this.getStack(list.get(start))) >= 0) {
                start++;
                infiniteLoop = false;
            }
            if (infiniteLoop) {
                throw new InfiniteLoopException("Trapped in an infinite loop while sorting items");
            }
            this.swap(list.get(start), list.get(end));
        }
        // 基准物品归位
        this.swap(list.getFirst(), list.get(start));
        sort(list.subList(0, start));
        sort(list.subList(start + 1, list.size()));
    }

    private void merge(int index, ItemStack itemStack) {
        for (int i = index + 1; i < this.size(); i++) {
            if (this.isValidSlot(i)) {
                ItemStack slotStack = this.getStack(i);
                if (slotStack.isEmpty()) {
                    continue;
                }
                if (InventoryUtils.canMergeTo(itemStack, slotStack)) {
                    InventoryUtils.mergeStack(itemStack, slotStack);
                    if (itemStack.isEmpty()) {
                        return;
                    }
                }
            }
        }
    }

    private boolean isValidSlot(int index) {
        ItemStack itemStack = this.getStack(index);
        if (itemStack.getCount() > itemStack.getMaxCount()) {
            return false;
        }
        if (itemStack == AbstractCustomSizeInventory.PLACEHOLDER) {
            return false;
        }
        if (InventoryUtils.isGcaItem(itemStack)) {
            return false;
        }
        if (fakePlayer.currentScreenHandler instanceof QuickShulkerScreenHandler quickShulkerScreenHandler) {
            return itemStack != quickShulkerScreenHandler.getShulkerBox();
        }
        return true;
    }

    public boolean replenishment(Predicate<ItemStack> predicate) {
        return this.replenishment(Hand.MAIN_HAND, predicate);
    }

    // TODO 需要测试
    public boolean replenishment(Hand hand, Predicate<ItemStack> predicate) {
        ItemStack stackInHand = this.getStack(hand);
        if (predicate.test(stackInHand)) {
            return true;
        }
        boolean pickItemFromShulker = CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.get();
        ArrayList<Integer> shulkers = new ArrayList<>();
        // 当前手槽位
        int headSlot = this.getHandSlotIndex(hand);
        for (int i = 0; i < this.size(); i++) {
            if (i == headSlot) {
                continue;
            }
            if (predicate.test(this.getStack(i))) {
                swap(i, headSlot);
                return true;
            } else if (pickItemFromShulker) {
                ItemStack shulker = this.getStack(i);
                if (shulker.isEmpty()) {
                    continue;
                }
                if (InventoryUtils.isShulkerBoxItem(shulker)) {
                    shulkers.add(i);
                }
            }
        }
        // 从潜影盒获取物品
        if (pickItemFromShulker) {
            for (Integer index : shulkers) {
                ItemStack shulker = this.getStack(index);
                ItemStack picked = InventoryUtils.pickItemFromShulkerBox(shulker, predicate);
                if (picked.isEmpty()) {
                    continue;
                }
                this.fakePlayer.setStackInHand(hand, picked);
                this.insertOrDropStack(stackInHand);
                return true;
            }
        }
        return false;
    }

    private int getHandSlotIndex(Hand hand) {
        return switch (hand) {
            case MAIN_HAND -> this.playerInventory.selectedSlot;
            case OFF_HAND -> 36;
        };
    }

    /**
     * 交换两个索引上的物品
     */
    private void swap(int first, int second) {
        ItemStack firstStack = this.getStack(first);
        ItemStack secondStack = this.getStack(second);
        this.setStack(first, secondStack);
        this.setStack(second, firstStack);
    }

    /**
     * 将该物品栏的索引映射到完整玩家物品栏上
     */
    private int map(int index) {
        int sum = 0;
        for (Map.Entry<Integer, DefaultedList<ItemStack>> entry : this.combine) {
            // 当前子物品集合在完整集合上对应的索引
            int start = entry.getKey();
            // 当前子物品集合的长度
            int size = entry.getValue().size();
            if (index - sum < size) {
                return start + index - sum;
            }
            sum += size;
        }
        throw new IndexOutOfBoundsException();
    }
}
