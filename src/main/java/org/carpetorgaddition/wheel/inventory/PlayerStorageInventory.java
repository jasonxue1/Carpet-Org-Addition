package org.carpetorgaddition.wheel.inventory;

import carpet.patches.EntityPlayerMPFake;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 一个不包括盔甲槽的玩家物品栏
 */
public class PlayerStorageInventory implements Container {
    private final Inventory playerInventory;
    private final EntityPlayerMPFake fakePlayer;
    private final IntList indexMapping;

    public PlayerStorageInventory(EntityPlayerMPFake fakePlayer) {
        this.playerInventory = fakePlayer.getInventory();
        this.fakePlayer = fakePlayer;
        IntArrayList list = new IntArrayList(37);
        NonNullList<ItemStack> main = this.playerInventory.getNonEquipmentItems();
        for (int i = 0; i < main.size(); i++) {
            list.add(i);
        }
        list.add(Inventory.SLOT_OFFHAND);
        this.indexMapping = new IntImmutableList(list);
    }

    @Override
    public int getContainerSize() {
        return this.indexMapping.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public @NonNull ItemStack getItem(int slot) {
        return this.playerInventory.getItem(this.map(slot));
    }

    public ItemStack getStack(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> this.fakePlayer.getMainHandItem();
            case OFF_HAND -> this.fakePlayer.getOffhandItem();
        };
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int amount) {
        return this.playerInventory.removeItem(this.map(slot), amount);
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        return this.playerInventory.removeItemNoUpdate(this.map(slot));
    }

    @Override
    public void setItem(int slot, @NonNull ItemStack stack) {
        this.playerInventory.setItem(this.map(slot), stack);
    }

    public void setStack(InteractionHand hand, ItemStack stack) {
        this.fakePlayer.setItemInHand(hand, stack);
    }

    @Override
    public void setChanged() {
        this.playerInventory.setChanged();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return this.playerInventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, ItemStack.EMPTY);
        }
    }

    public void drop(int index) {
        ItemStack itemStack = this.getItem(index);
        this.setItem(index, ItemStack.EMPTY);
        this.fakePlayer.drop(itemStack, false, true);
    }

    /**
     * 根据条件丢弃物品栏中所有物品
     *
     * @return 是否丢弃了物品
     */
    public boolean drop(Predicate<ItemStack> predicate) {
        boolean dropped = false;
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (predicate.test(itemStack)) {
                this.drop(i);
                dropped = true;
            }
        }
        return dropped;
    }

    /**
     * 向物品栏中插入物品，优先插入到潜影盒物品中
     *
     * @return 物品栏是否满了
     */
    public boolean insertWithShulkerBoxPriority(ItemStack itemStack) {
        ItemStack remaining = insertToShulkerBox(itemStack);
        if (remaining.isEmpty()) {
            return false;
        }
        // 潜影盒没有足够空间，插入到物品栏中
        this.insert(remaining);
        if (remaining.isEmpty()) {
            return false;
        }
        // 潜影盒和物品栏都没有足够空间，丢弃物品
        FakePlayerUtils.dropItem(this.fakePlayer, remaining);
        return true;
    }

    /**
     * 向物品栏中插入物品，优先插入到物品栏中
     */
    public void insertWithInventoryPriority(ItemStack itemStack) {
        this.insert(itemStack);
        if (itemStack.isEmpty()) {
            return;
        }
        // 物品栏中没有足够的空间，插入到潜影盒中
        ItemStack remaining = this.insertToShulkerBox(itemStack);
        if (remaining.isEmpty()) {
            return;
        }
        // 物品栏和潜影盒都没有足够空间，丢弃物品
        FakePlayerUtils.dropItem(this.fakePlayer, remaining);
    }

    /**
     * 将物品插入到潜影盒
     *
     * @return 物品剩余未插入的部分
     */
    @NotNull
    @CheckReturnValue
    private ItemStack insertToShulkerBox(ItemStack itemStack) {
        if (CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.get()) {
            itemStack = itemStack.copyAndClear();
            // 所有潜影盒所在的索引
            ArrayList<Integer> shulkers = new ArrayList<>();
            for (int i = 0; i < this.getContainerSize(); i++) {
                ItemStack shulker = this.getItem(i);
                if (InventoryUtils.isShulkerBoxItem(shulker)) {
                    shulkers.add(i);
                    // 优先尝试向单一物品的潜影盒或杂物潜影盒装入物品
                    if (InventoryUtils.canAcceptAsSingleItemType(shulker, itemStack, false) || InventoryUtils.isJunkBox(shulker)) {
                        itemStack = InventoryUtils.addItemToShulkerBox(shulker, itemStack);
                        if (itemStack.isEmpty()) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            // 尝试向空潜影盒装入物品
            for (Integer index : shulkers) {
                ItemStack shulker = this.getItem(index);
                if (InventoryUtils.canAcceptAsSingleItemType(shulker, itemStack, true)) {
                    itemStack = InventoryUtils.addItemToShulkerBox(shulker, itemStack);
                    if (itemStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            if (!shulkers.isEmpty()) {
                // 拆分堆叠的空潜影盒
                int last = shulkers.getLast();
                if (last < this.getContainerSize() - 1 && this.getItem(last).getCount() > 1) {
                    this.splitStackedShulkerBox(last, last + 1);
                    itemStack = InventoryUtils.addItemToShulkerBox(this.getItem(last + 1), itemStack);
                }
            }
        }
        return itemStack;
    }

    /**
     * 向物品栏中插入物品
     */
    public void insert(ItemStack itemStack) {
        this.playerInventory.add(itemStack);
        if (itemStack.isEmpty()) {
            return;
        }
        ItemStack offHandStack = this.getStack(InteractionHand.OFF_HAND);
        if (offHandStack.isEmpty()) {
            this.setStack(InteractionHand.OFF_HAND, itemStack.copyAndClear());
        } else if (InventoryUtils.canMergeTo(itemStack, offHandStack)) {
            InventoryUtils.mergeStack(itemStack, offHandStack);
        }
    }

    /**
     * 拆分堆叠的潜影盒
     */
    private void splitStackedShulkerBox(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return;
        }
        ItemStack from = this.getItem(fromIndex);
        ItemStack to = this.getItem(toIndex);
        if (to.isEmpty()) {
            ItemStack split = from.split(1);
            this.setItem(toIndex, split);
        }
    }

    /**
     * 整理物品栏
     */
    public void sort() {
        // 记录所有未被锁定的槽位
        ArrayList<Integer> list = new ArrayList<>();
        // 合并相同的物品
        for (int index = 0; index < this.getContainerSize(); index++) {
            if (this.isValidSlot(index)) {
                list.add(index);
                ItemStack itemStack = this.getItem(index);
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
        ItemStack pivot = this.getItem(list.getFirst());
        while (start < end) {
            // 程序是否陷入了死循环
            boolean infiniteLoop = true;
            while (end > start && InventoryUtils.compare(pivot, this.getItem(list.get(end))) <= 0) {
                end--;
                infiniteLoop = false;
            }
            while (end > start && InventoryUtils.compare(pivot, this.getItem(list.get(start))) >= 0) {
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
        for (int i = index + 1; i < this.getContainerSize(); i++) {
            if (this.isValidSlot(i)) {
                ItemStack slotStack = this.getItem(i);
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

    /**
     * @return 指定索引的物品是否可以被操作
     */
    private boolean isValidSlot(int index) {
        ItemStack itemStack = this.getItem(index);
        if (itemStack.getCount() > itemStack.getMaxStackSize()) {
            return false;
        }
        if (itemStack == AbstractCustomSizeInventory.PLACEHOLDER) {
            return false;
        }
        if (InventoryUtils.isGcaItem(itemStack)) {
            return false;
        }
        if (fakePlayer.containerMenu instanceof QuickShulkerScreenHandler quickShulkerScreenHandler) {
            return itemStack != quickShulkerScreenHandler.getShulkerBox();
        }
        return true;
    }

    /**
     * 将指定物品栏移动到主手
     *
     * @return 是否移动成功
     */
    public boolean replenishment(Predicate<ItemStack> predicate) {
        return this.replenishment(InteractionHand.MAIN_HAND, predicate);
    }

    public boolean replenishment(InteractionHand hand, Predicate<ItemStack> predicate) {
        ItemStack stackInHand = this.getStack(hand);
        if (predicate.test(stackInHand)) {
            return true;
        }
        boolean pickItemFromShulker = CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.get();
        ArrayList<Integer> shulkers = new ArrayList<>();
        // 当前手槽位
        int headSlot = this.getHandSlotIndex(hand);
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (i == headSlot) {
                continue;
            }
            if (predicate.test(this.getItem(i))) {
                swap(i, headSlot);
                return true;
            } else if (pickItemFromShulker) {
                ItemStack shulker = this.getItem(i);
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
                ItemStack shulker = this.getItem(index);
                ItemStack picked = InventoryUtils.pickItemFromShulkerBox(shulker, predicate);
                if (picked.isEmpty()) {
                    continue;
                }
                this.fakePlayer.setItemInHand(hand, picked);
                this.insertWithInventoryPriority(stackInHand);
                return true;
            }
        }
        return false;
    }

    private int getHandSlotIndex(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> this.playerInventory.getSelectedSlot();
            case OFF_HAND -> 36;
        };
    }

    /**
     * @return 物品栏中是否包含指定物品
     */
    public boolean contains(Predicate<ItemStack> predicate) {
        boolean pickItemFromShulker = CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.get();
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (predicate.test(itemStack)) {
                return true;
            }
            if (pickItemFromShulker && InventoryUtils.contains(itemStack, predicate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 交换两个索引上的物品
     */
    private void swap(int first, int second) {
        if (first == second) {
            return;
        }
        ItemStack firstStack = this.getItem(first);
        ItemStack secondStack = this.getItem(second);
        this.setItem(first, secondStack);
        this.setItem(second, firstStack);
    }

    /**
     * 将该物品栏的索引映射到完整玩家物品栏上
     */
    private int map(int index) {
        return this.indexMapping.getInt(index);
    }
}
