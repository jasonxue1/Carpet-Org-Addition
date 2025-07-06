package org.carpetorgaddition.wheel;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Predicate;

/**
 * 用来统计一个物品栏内的物品数量
 */
public class ItemStackStatistics {
    /**
     * 统计到的物品总数
     */
    private int sum;
    /**
     * 物品谓词
     */
    private final Predicate<ItemStack> predicate;
    /**
     * 每一种物品的数量
     */
    private final Counter<Item> counter = new Counter<>();
    /**
     * 用来记录哪些物品是（或有些是）从嵌套的物品栏中找到的
     */
    private final HashSet<Item> nestingItem = new HashSet<>();

    public ItemStackStatistics(Predicate<ItemStack> predicate) {
        this.predicate = predicate;
    }

    /**
     * 统计物品栏内指定物品的数量<br>
     * 如果物品栏内包含容器物品或者收纳袋，则同时统计嵌套的物品数量
     */
    public void statistics(Inventory inventory) {
        this.statistics(inventory, false);
    }

    /**
     * 统计物品栏内指定物品的数量<br>
     * 如果物品栏内包含容器物品或者收纳袋，则同时统计嵌套的物品数量
     *
     * @param isNestingInventory 当前物品栏是否是潜影盒或收纳袋内部的物品栏
     */
    public void statistics(Inventory inventory, boolean isNestingInventory) {
        for (int i = 0; i < inventory.size(); i++) {
            this.tally(inventory.getStack(i), isNestingInventory);
        }
    }

    public void statistics(Iterable<ItemStack> iterable, boolean isNestingInventory, int multiple) {
        iterable.forEach(itemStack -> this.tally(itemStack, isNestingInventory, multiple));
    }

    private void statistics(ItemStack itemStack) {
        // 容器物品
        int count = itemStack.getCount();
        ContainerComponent container = itemStack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            this.statistics(container.iterateNonEmpty(), true, count);
            // 不考虑一个物品同时有容器物品组件和收纳袋物品组件的情况
            return;
        }
        // 收纳袋物品
        BundleContentsComponent bundleContents = itemStack.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (bundleContents != null) {
            this.statistics(bundleContents.iterate(), true, count);
        }
    }

    /**
     * 累加该物品的数量
     *
     * @param itemStack 物品和物品的数量
     * @param nesting   该物品是否是从嵌套的容器中获取的
     */
    private void tally(ItemStack itemStack, boolean nesting) {
        this.tally(itemStack, nesting, 1);
    }

    /**
     * 累加该物品的数量
     *
     * @param itemStack 物品和物品的数量
     * @param nesting   该物品是否是从嵌套的容器中获取的
     */
    private void tally(ItemStack itemStack, boolean nesting, int multiple) {
        if (this.predicate.test(itemStack)) {
            Item item = itemStack.getItem();
            int count = itemStack.getCount();
            this.counter.add(item, count * multiple);
            this.sum += (count * multiple);
            if (nesting) {
                this.nestingItem.add(item);
            }
        }
        this.statistics(itemStack);
    }

    public int getSum() {
        return this.sum;
    }

    public boolean hasNestingItem() {
        return !this.nestingItem.isEmpty();
    }

    public Text getCountText() {
        ArrayList<Text> list = new ArrayList<>();
        for (Item item : this.counter) {
            MutableText itemCount = itemCount(this.counter.getCount(item), item.getMaxCount());
            if (this.nestingItem.contains(item)) {
                TextBuilder builder = TextBuilder.fromCombined(item.getName(), " ", itemCount);
                builder.setItalic();
                list.add(builder.build());
            } else {
                list.add(TextBuilder.combineAll(item.getName(), " ", itemCount));
            }
        }
        MutableText text = TextBuilder.create(this.getSum());
        TextBuilder builder = new TextBuilder(text);
        builder.setHover(TextBuilder.joinList(list));
        return this.nestingItem.isEmpty() ? builder.build() : builder.setItalic().build();
    }

    private MutableText itemCount(int count, int maxCount) {
        // 计算物品有多少组
        int group = count / maxCount;
        // 计算物品余几个
        int remainder = count % maxCount;
        // 为文本添加悬停提示
        if (group == 0) {
            return TextBuilder.translate("carpet.command.item.remainder", remainder);
        } else if (remainder == 0) {
            return TextBuilder.translate("carpet.command.item.group", group);
        } else {
            return TextBuilder.translate("carpet.command.item.count", group, remainder);
        }
    }
}
