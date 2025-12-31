package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.wheel.ContainerDeepCopy;
import boat.carpetorgaddition.wheel.Counter;
import boat.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import boat.carpetorgaddition.wheel.inventory.ImmutableInventory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class InventoryUtils {
    /**
     * 模组{@code gugle-carpet-addition}是否已加载
     */
    private static final boolean GCA_LOADED = FabricLoader.getInstance().isModLoaded("gca");

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
        if (isOperableSulkerBox(shulkerBox)) {
            // 将潜影盒内的物品栏组件替换为该组件的深拷贝副本
            InventoryUtils.deepCopyContainer(shulkerBox);
            ContainerComponentInventory inventory = new ContainerComponentInventory(shulkerBox);
            return inventory.pinkStack(predicate);
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
        if (isOperableSulkerBox(shulkerBox)) {
            // 将潜影盒内的物品栏组件替换为该组件的深拷贝副本
            InventoryUtils.deepCopyContainer(shulkerBox);
            ContainerComponentInventory inventory = new ContainerComponentInventory(shulkerBox);
            return inventory.pinkStack(predicate, count);
        }
        return ItemStack.EMPTY;
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
        if (isOperableSulkerBox(container) && itemStack.getItem().canFitInsideContainerItems()) {
            ContainerComponentInventory inventory = new ContainerComponentInventory(container);
            return inventory.addItem(itemStack);
        }
        return itemStack;
    }

    /**
     * @return 潜影盒中可以插入多少个物品
     */
    public static int shulkerCanInsertItemCount(ItemStack container, ItemStack itemStack) {
        ItemContainerContents component = container.get(DataComponents.CONTAINER);
        if (component == null || component == ItemContainerContents.EMPTY) {
            return itemStack.getMaxStackSize() * ContainerComponentInventory.CONTAINER_SIZE;
        }
        List<ItemStack> list = component.nonEmptyStream().toList();
        int count = 0;
        for (ItemStack stack : list) {
            if (stack.getCount() == stack.getMaxStackSize()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(stack, itemStack)) {
                count += (stack.getMaxStackSize() - stack.getCount());
            }
        }
        int emptySlotCount = ContainerComponentInventory.CONTAINER_SIZE - list.size();
        count += emptySlotCount * itemStack.getMaxStackSize();
        return count;
    }

    /**
     * @return 获取潜影盒中的第一个物品
     */
    @Contract(pure = true)
    public static ItemStack getFirstItemStack(ItemStack container) {
        if (isOperableSulkerBox(container)) {
            ItemContainerContents component = container.get(DataComponents.CONTAINER);
            if (component == null || component == ItemContainerContents.EMPTY) {
                return ItemStack.EMPTY;
            }
            Iterator<ItemStack> iterator = component.nonEmptyStream().iterator();
            return iterator.hasNext() ? iterator.next() : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
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
        ItemContainerContents component = shulkerBox.get(DataComponents.CONTAINER);
        if (component == null || component == ItemContainerContents.EMPTY) {
            return true;
        }
        return !component.nonEmptyItems().iterator().hasNext();
    }

    /**
     * @return 潜影盒中是否有指定物品
     */
    public static boolean contains(ItemStack shulkerBox, Predicate<ItemStack> predicate) {
        if (isOperableSulkerBox(shulkerBox)) {
            ContainerComponentInventory inventory = new ContainerComponentInventory(shulkerBox);
            for (ItemStack itemStack : inventory) {
                if (predicate.test(itemStack)) {
                    return true;
                }
            }
        }
        return false;
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
        ItemContainerContents component = shulkerBox.get(DataComponents.CONTAINER);
        // 因为有空潜影盒的判断，shulkerBox.get(DataComponentTypes.CONTAINER)不会返回null
        //noinspection DataFlowIssue
        return new ImmutableInventory(component.nonEmptyStream().toList());
    }


    /**
     * 在创造模式下使用鼠标中键复制的物品时，物品组件只是被浅拷贝了，这些被复制的物品还是共享同一个组件地址，当直接对其中一个组件进行操作时，所有被复制的物品都会受到影响，换句话说，当其中一个潜影盒中的物品被本类中的方法取出来后，所有被复制的潜影盒中这个物品都会消失，假玩家也就不能正确的从潜影盒中拿取物品。所以本方法的作用是将物品组件替换为它的深克隆对象。
     *
     * @param shulkerBox 要替换组件的潜影盒
     * @see <a href="https://bugs.mojang.com/browse/MC-271123">MC-271123</a>
     */
    public static void deepCopyContainer(ItemStack shulkerBox) {
        ItemContainerContents component = shulkerBox.get(DataComponents.CONTAINER);
        if (component == null || component == ItemContainerContents.EMPTY) {
            return;
        }
        ItemContainerContents copy = ((ContainerDeepCopy) (Object) component).carpet_Org_Addition$copy();
        shulkerBox.set(DataComponents.CONTAINER, copy);
    }

    /**
     * 判断指定物品是否为潜影盒
     *
     * @param shulkerBox 要判断是否为潜影盒的物品
     * @return 指定物品是否是潜影盒
     */
    public static boolean isShulkerBoxItem(ItemStack shulkerBox) {
        if (shulkerBox.is(Items.SHULKER_BOX)) {
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
        return itemStack.isStackable() && itemStack.getMaxStackSize() <= itemStack.getCount();
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
        return ItemStack.isSameItemSameComponents(stack, otherStack);
    }

    /**
     * 合并两个相同的物品
     */
    public static void mergeStack(ItemStack sacrifice, ItemStack retain) {
        if (canMerge(sacrifice, retain)) {
            if (isItemStackFull(retain)) {
                return;
            }
            int shortage = Math.min(retain.getMaxStackSize() - retain.getCount(), sacrifice.getCount());
            retain.grow(shortage);
            sacrifice.shrink(shortage);
        } else {
            throw new IllegalArgumentException("Attempting to merge two items that are not completely identical");
        }
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
        if (left.is(right.getItem())) {
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
            return Integer.compare(ItemStack.hashItemAndComponents(left), ItemStack.hashItemAndComponents(right));
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
    public static ItemStack findMostAbundantStack(Container inventory, Predicate<ItemStack> predicate) {
        Counter<Counter.Wrapper<ItemStack>> counter = new Counter<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
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
        ItemContainerContents component = shulker.get(DataComponents.CONTAINER);
        if (component == null || component == ItemContainerContents.EMPTY) {
            return acceptEmptyShulker;
        }
        return component.nonEmptyStream().allMatch(itemStack -> canMerge(itemStack, target));
    }

    /**
     * @return 潜影盒是否包含多种物品
     */
    public static boolean isJunkBox(ItemStack shulker) {
        ItemContainerContents component = shulker.get(DataComponents.CONTAINER);
        if (component == null || component == ItemContainerContents.EMPTY) {
            return false;
        }
        List<ItemStack> list = component.nonEmptyStream().toList();
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
     * @return 指定潜影盒是否可以存取物品
     */
    public static boolean isOperableSulkerBox(ItemStack shulker) {
        return isShulkerBoxItem(shulker) && shulker.getCount() == 1;
    }

    /**
     * 统计物品栏内指定物品的数量
     */
    public static int count(Container inventory, Predicate<ItemStack> predicate) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (predicate.test(itemStack)) {
                count += itemStack.getCount();
            }
        }
        return count;
    }

    public static String getRegistryId(Item item) {
        return item.toString();
    }

    /**
     * @return 指定物品是否是食物
     */
    public static boolean isFoodItem(ItemStack itemStack) {
        return itemStack.has(DataComponents.FOOD);
    }

    /**
     * @return 指定物品是否是头盔
     */
    public static boolean isHelmetItem(ItemStack itemStack) {
        return canBeEquipped(itemStack, EquipmentSlot.HEAD);
    }

    /**
     * @return 指定物品是否是胸甲
     */
    public static boolean isChestplateItem(ItemStack itemStack) {
        return canBeEquipped(itemStack, EquipmentSlot.CHEST);
    }

    /**
     * @return 指定物品是否是护腿
     */
    public static boolean isLeggingsItem(ItemStack itemStack) {
        return canBeEquipped(itemStack, EquipmentSlot.LEGS);
    }

    /**
     * @return 指定物品是否是靴子
     */
    public static boolean isBootsItem(ItemStack itemStack) {
        return canBeEquipped(itemStack, EquipmentSlot.FEET);
    }

    /**
     * @return 指定物品是否可以穿戴到指定槽位上
     */
    private static boolean canBeEquipped(ItemStack itemStack, EquipmentSlot slot) {
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }
        return slot == equippable.slot();
    }

    /**
     * @return 指定物品是否是工具
     */
    public static boolean isToolItem(ItemStack itemStack) {
        return itemStack.has(DataComponents.TOOL);
    }

    /**
     * @return 指定物品是否有不死图腾效果
     */
    public static boolean isTotemItem(ItemStack itemStack) {
        return itemStack.has(DataComponents.DEATH_PROTECTION);
    }

    /**
     * 指定物品是否为{@code GCA}（假人背包）物品
     */
    public static boolean isGcaItem(ItemStack itemStack) {
        if (GCA_LOADED || CarpetOrgAddition.isDebugDevelopment()) {
            CustomData component = itemStack.get(DataComponents.CUSTOM_DATA);
            if (component == null) {
                return false;
            }
            return component.copyTag().get("GcaClear") != null;
        }
        return false;
    }

    public static class ItemStackWrapper extends Counter.Wrapper<ItemStack> {
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
            return ItemStack.hashItemAndComponents(value);
        }
    }
}
