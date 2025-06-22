package org.carpetorgaddition.util;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.carpetorgaddition.wheel.ContainerDeepCopy;
import org.carpetorgaddition.wheel.Counter;
import org.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import org.carpetorgaddition.wheel.inventory.ImmutableInventory;
import org.jetbrains.annotations.CheckReturnValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class InventoryUtils {
    /**
     * 物品栏工具类，私有化构造方法
     */
    private InventoryUtils() {
    }

    /**
     * 从物品形式的潜影盒中获取第一个指定的物品，并将该物品从潜影盒的NBT中删除，使用时，为避免不必要的物品浪费，取出来的物品必须使用或丢出
     *
     * @param shulkerBox 潜影盒物品
     * @param predicate  一个物品匹配器对象，用来指定要从潜影盒中拿取的物品
     * @return 潜影盒中获取的指定物品
     */
    @CheckReturnValue
    public static ItemStack pickItemFromShulkerBox(ItemStack shulkerBox, Predicate<ItemStack> predicate) {
        // 判断潜影盒是否为空，空潜影盒直接返回空物品
        if (isEmptyShulkerBox(shulkerBox)) {
            return ItemStack.EMPTY;
        }
        // 将潜影盒内的物品栏组件替换为该组件的深拷贝副本
        InventoryUtils.deepCopyContainer(shulkerBox);
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        //noinspection DataFlowIssue
        for (ItemStack itemStack : component.iterateNonEmpty()) {
            if (predicate.test(itemStack)) {
                ItemStack copy = itemStack.copy();
                itemStack.setCount(0);
                removeContainerIfEmpty(itemStack);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 从潜影盒中取出指定数量的物品
     *
     * @return 从潜影盒中取出的物品
     */
    @CheckReturnValue
    public static ItemStack pickItemFromShulkerBox(ItemStack shulkerBox, Predicate<ItemStack> predicate, int count) {
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        // 将潜影盒内的物品栏组件替换为该组件的深拷贝副本
        InventoryUtils.deepCopyContainer(shulkerBox);
        ContainerComponent component = shulkerBox.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        ItemStack itemStack = ItemStack.EMPTY;
        for (ItemStack stack : component.iterateNonEmpty()) {
            if (itemStack.isEmpty()) {
                if (predicate.test(stack)) {
                    itemStack = stack.split(count);
                    // 如果count为65，而物品最大堆叠数为64，那么取出的物品数量将超过最大值
                    // 因此在这里限制一下count的值
                    count = Math.min(itemStack.getMaxCount(), count) - itemStack.getCount();
                }
            } else if (ItemStack.areItemsAndComponentsEqual(itemStack, stack)) {
                ItemStack split = stack.split(count);
                count -= split.getCount();
                itemStack.increment(split.getCount());
            }
            if (count == 0) {
                break;
            }
            if (count < 0) {
                throw new IllegalStateException();
            }
        }
        return itemStack;
    }

    /**
     * 将物品填充到容器物品中
     *
     * @param container 容器物品
     * @param itemStack 要填充的物品
     * @return 剩余物品
     */
    @CheckReturnValue
    public static ItemStack addItemToShulkerBox(ItemStack container, ItemStack itemStack) {
        if (container.isEmpty() || itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isShulkerBoxItem(container) && container.getCount() == 1 && itemStack.getItem().canBeNested()) {
            ContainerComponentInventory inventory = new ContainerComponentInventory(container);
            return inventory.addStack(itemStack);
        }
        return itemStack;
    }

    // TODO 添加规则开关
    @CheckReturnValue
    public static ItemStack putItemToInventoryShulkerBox(ItemStack itemStack, PlayerInventory inventory) {
        itemStack = itemStack.copyAndEmpty();
        // 所有潜影盒所在的索引
        ArrayList<Integer> shulkers = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack shulker = inventory.getStack(i);
            if (isShulkerBoxItem(shulker)) {
                shulkers.add(i);
                // 优先尝试向单一物品的潜影盒或杂物潜影盒装入物品
                if (canAcceptAsSingleItemType(shulker, itemStack, false) || isJunkBox(shulker)) {
                    itemStack = addItemToShulkerBox(shulker, itemStack);
                    if (itemStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        // 尝试向空潜影盒装入物品
        for (Integer index : shulkers) {
            ItemStack shulker = inventory.getStack(index);
            if (canAcceptAsSingleItemType(shulker, itemStack, true)) {
                itemStack = addItemToShulkerBox(shulker, itemStack);
                if (itemStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return itemStack;
    }

    /**
     * @return 潜影盒中可以插入多少个物品
     */
    public static int shulkerCanInsertItemCount(ItemStack container, ItemStack itemStack) {
        ContainerComponent component = container.get(DataComponentTypes.CONTAINER);
        if (component == null || component == ContainerComponent.DEFAULT) {
            return itemStack.getMaxCount() * ContainerComponentInventory.CONTAINER_SIZE;
        }
        List<ItemStack> list = component.streamNonEmpty().toList();
        int count = 0;
        for (ItemStack stack : list) {
            if (stack.getCount() == stack.getMaxCount()) {
                continue;
            }
            if (ItemStack.areItemsAndComponentsEqual(stack, itemStack)) {
                count += (stack.getMaxCount() - stack.getCount());
            }
        }
        int emptySlotCount = ContainerComponentInventory.CONTAINER_SIZE - list.size();
        count += emptySlotCount * itemStack.getMaxCount();
        return count;
    }

    /**
     * 获取潜影盒中指定物品，并让这个物品执行一个函数，然后将执行函数前的物品返回
     *
     * @param predicate 匹配物品的谓词
     * @param consumer  要执行的函数
     * @return 执行函数前的物品
     */
    public static ItemStack shulkerBoxConsumer(ItemStack shulkerBox, Predicate<ItemStack> predicate, Consumer<ItemStack> consumer) {
        if (isEmptyShulkerBox(shulkerBox)) {
            // 因为这个判断，可以保证下方的shulkerBox.get(DataComponentTypes.CONTAINER)不会返回null
            return ItemStack.EMPTY;
        }
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        // noinspection DataFlowIssue
        for (ItemStack stack : component.iterateNonEmpty()) {
            if (predicate.test(stack)) {
                ItemStack copyStack = stack.copy();
                consumer.accept(stack);
                return copyStack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * @return 获取潜影盒中的第一个物品
     */
    public static ItemStack getFirstItemStack(ItemStack container) {
        if (isShulkerBoxItem(container) && container.getCount() == 1) {
            ContainerComponent component = container.get(DataComponentTypes.CONTAINER);
            if (component == null || component == ContainerComponent.DEFAULT) {
                return ItemStack.EMPTY;
            }
            Iterator<ItemStack> iterator = component.streamNonEmpty().iterator();
            return iterator.hasNext() ? iterator.next() : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    public static boolean isOf(ItemStack itemStack, Item... items) {
        for (Item item : items) {
            if (itemStack.isOf(item)) {
                return true;
            }
        }
        return false;
    }

    // 如果潜影盒为空，删除对应的NBT
    private static void removeContainerIfEmpty(ItemStack shulkerBox) {
        if (isEmptyShulkerBox(shulkerBox)) {
            // 如果潜影盒最后一个物品被取出，就删除潜影盒的物品栏数据堆叠组件以保证潜影盒堆叠的正常运行
            shulkerBox.set(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        }
    }

    /**
     * 判断当前潜影盒是否是空潜影盒
     *
     * @param shulkerBox 当前要检查是否为空的潜影盒物品
     * @return 潜影盒内没有物品返回true，有物品返回false
     * @apiNote 此方法可以保证返回值为false时，shulkerBox.get(DataComponentTypes.CONTAINER)永远不会返回null
     */
    public static boolean isEmptyShulkerBox(ItemStack shulkerBox) {
        // 正常情况下有物品的潜影盒无法堆叠
        if (shulkerBox.getCount() != 1) {
            return true;
        }
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        if (component == null || component == ContainerComponent.DEFAULT) {
            return true;
        }
        return !component.iterateNonEmpty().iterator().hasNext();
    }

    /**
     * 获取潜影盒物品的物品栏
     *
     * @param shulkerBox 要获取物品栏的潜影盒
     * @return 潜影盒内的物品栏
     */
    public static ImmutableInventory getInventory(ItemStack shulkerBox) {
        if (isEmptyShulkerBox(shulkerBox)) {
            return ImmutableInventory.EMPTY;
        }
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        // 因为有空潜影盒的判断，shulkerBox.get(DataComponentTypes.CONTAINER)不会返回null
        //noinspection DataFlowIssue
        return new ImmutableInventory(component.streamNonEmpty().toList());
    }


    /**
     * 在创造模式下使用鼠标中键复制的物品时，物品组件只是被浅拷贝了，这些被复制的物品还是共享同一个组件地址，当直接对其中一个组件进行操作时，所有被复制的物品都会受到影响，换句话说，当其中一个潜影盒中的物品被本类中的方法取出来后，所有被复制的潜影盒中这个物品都会消失，假玩家也就不能正确的从潜影盒中拿取物品。所以本方法的作用是将物品组件替换为它的深克隆对象。
     *
     * @param shulkerBox 要替换组件的潜影盒
     * @see <a href="https://bugs.mojang.com/browse/MC-271123">MC-271123</a>
     */
    public static void deepCopyContainer(ItemStack shulkerBox) {
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        if (component == null || component == ContainerComponent.DEFAULT) {
            return;
        }
        ContainerComponent copy = ((ContainerDeepCopy) (Object) component).copy();
        shulkerBox.set(DataComponentTypes.CONTAINER, copy);
    }

    /**
     * 判断指定物品是否为潜影盒
     *
     * @param shulkerBox 要判断是否为潜影盒的物品
     * @return 指定物品是否是潜影盒
     */
    public static boolean isShulkerBoxItem(ItemStack shulkerBox) {
        if (shulkerBox.isOf(Items.SHULKER_BOX)) {
            return true;
        }
        if (shulkerBox.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock() instanceof ShulkerBoxBlock;
        }
        return false;
    }

    /**
     * 断言指定物品为空
     *
     * @param itemStack 被断言的物品
     */
    public static void assertEmptyStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        throw new IllegalStateException();
    }

    /**
     * 断言指定物品为空
     *
     * @param itemStack 被断言的物品
     * @param message   异常消息
     */
    public static void assertEmptyStack(ItemStack itemStack, Supplier<String> message) {
        if (itemStack.isEmpty()) {
            return;
        }
        throw new IllegalStateException(message.get());
    }

    /**
     * @return 物品是否已经堆叠满
     */
    public static boolean isItemStackFull(ItemStack itemStack) {
        return itemStack.isStackable() && itemStack.getMaxCount() <= itemStack.getCount();
    }

    /**
     * 指定物品是否可以合并到目标物品
     */
    public static boolean canMergeTo(ItemStack stack, ItemStack target) {
        if (isItemStackFull(target)) {
            return false;
        }
        return canMerge(stack, target);
    }

    public static boolean canMerge(ItemStack stack, ItemStack otherStack) {
        return ItemStack.areItemsAndComponentsEqual(stack, otherStack);
    }

    /**
     * @return 如果物品相等，返回{@code 0}，如果{@code left}在{@code right}之前，返回{@code -1}，否则返回{@code 1}
     */
    public static int compare(ItemStack left, ItemStack right) {
        // 物品完全相同，按照数量排序
        if (canMerge(left, right)) {
            return -Integer.compare(left.getCount(), right.getCount());
        }
        // 空物品放在最后
        if (left.isEmpty()) {
            return 1;
        }
        if (right.isEmpty()) {
            return -1;
        }
        if (left.isOf(right.getItem())) {
            // 两个物品都是潜影盒
            if (InventoryUtils.isShulkerBoxItem(left)) {
                return new ContainerComponentInventory(left).compareTo(new ContainerComponentInventory(right));
            }
            // 物品类型相同，物品组件少的排在前面
            int compareComponent = Integer.compare(left.getComponents().size(), right.getComponents().size());
            if (compareComponent != 0) {
                return compareComponent;
            }
            int compareCount = -Integer.compare(left.getCount(), right.getCount());
            if (compareCount != 0) {
                return compareComponent;
            }
            return Integer.compare(ItemStack.hashCode(left), ItemStack.hashCode(right));
        } else {
            // 潜影盒放在普通物品后面，忽略潜影盒颜色
            if (isShulkerBoxItem(left) && !isShulkerBoxItem(right)) {
                return 1;
            }
            if (!isShulkerBoxItem(left) && isShulkerBoxItem(right)) {
                return -1;
            }
            // 物品不相同，按照物品ID排序
            return compare(left.getItem(), right.getItem());
        }
    }

    public static int compare(Item left, Item right) {
        return getRegistryId(left).compareTo(getRegistryId(right));
    }

    /**
     * 根据条件获取物品栏中数量最多的物品
     */
    public static ItemStack findMostAbundantStack(Inventory inventory, Predicate<ItemStack> predicate) {
        Counter<Counter.Wrapper<ItemStack>> counter = new Counter<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (predicate.test(itemStack)) {
                counter.add(new ItemStackWrapper(itemStack), itemStack.getCount());
            }
        }
        return counter.getMostOrDefault(ItemStackWrapper.EMPTY).getValue().copy();
    }

    /**
     * @param acceptEmptyShulker 是否可以向空潜影盒插入物品
     * @return 潜影盒中是否有且只有一种物品，并且要插入的物品与潜影盒中的物品相同
     */
    public static boolean canAcceptAsSingleItemType(ItemStack shulker, ItemStack target, boolean acceptEmptyShulker) {
        ContainerComponent component = shulker.get(DataComponentTypes.CONTAINER);
        if (component == null || component == ContainerComponent.DEFAULT) {
            return acceptEmptyShulker;
        }
        return component.streamNonEmpty().allMatch(itemStack -> canMerge(itemStack, target));
    }

    /**
     * @return 潜影盒是否包含多种物品
     */
    public static boolean isJunkBox(ItemStack shulker) {
        ContainerComponent component = shulker.get(DataComponentTypes.CONTAINER);
        if (component == null || component == ContainerComponent.DEFAULT) {
            return false;
        }
        List<ItemStack> list = component.streamNonEmpty().toList();
        // 潜影盒为空或只有一个物品
        if (list.size() < 2) {
            return false;
        }
        ItemStack first = list.getFirst();
        for (int i = 1; i < list.size(); i++) {
            if (InventoryUtils.canMerge(first, list.get(i))) {
                continue;
            }
            return true;
        }
        return false;
    }


    /**
     * 统计物品栏内指定物品的数量
     */
    public static int count(Inventory inventory, Predicate<ItemStack> predicate) {
        int count = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (predicate.test(itemStack)) {
                count += itemStack.getCount();
            }
        }
        return count;
    }

    public static String getRegistryId(Item item) {
        return item.toString();
    }

    public static boolean isFoodItem(ItemStack itemStack) {
        return itemStack.contains(DataComponentTypes.FOOD);
    }

    public static boolean isToolItem(ItemStack itemStack) {
        return itemStack.contains(DataComponentTypes.TOOL);
    }

    private static class ItemStackWrapper extends Counter.Wrapper<ItemStack> {
        private static final ItemStackWrapper EMPTY = new ItemStackWrapper(ItemStack.EMPTY);

        public ItemStackWrapper(ItemStack value) {
            super(value);
        }

        @Override
        public boolean valueEquals(ItemStack value1, Object value2) {
            if (value1.getClass() == value2.getClass()) {
                return InventoryUtils.canMerge(value1, (ItemStack) value2);
            }
            return false;
        }

        @Override
        public int valueHashCode(ItemStack value) {
            return ItemStack.hashCode(value);
        }
    }
}
