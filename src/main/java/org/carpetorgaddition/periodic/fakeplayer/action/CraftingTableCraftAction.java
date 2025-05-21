package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.inventory.AutoGrowInventory;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.ArrayList;

public class CraftingTableCraftAction extends AbstractPlayerAction {
    /**
     * 合成配方
     */
    private final ItemStackPredicate[] predicates = new ItemStackPredicate[9];

    public CraftingTableCraftAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate[] predicates) {
        super(fakePlayer);
        if (predicates.length != 9) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(predicates, 0, this.predicates, 0, this.predicates.length);
    }

    @Override
    public void tick() {
        if (this.fakePlayer.currentScreenHandler instanceof CraftingScreenHandler craftingScreenHandler) {
            AutoGrowInventory inventory = new AutoGrowInventory();
            this.craftingTableCraft(inventory, craftingScreenHandler);
            // 丢弃合成输出
            for (ItemStack itemStack : inventory) {
                this.fakePlayer.dropItem(itemStack, false, true);
            }
        }
    }

    private void craftingTableCraft(AutoGrowInventory inventory, CraftingScreenHandler craftingScreenHandler) {
        // 定义变量记录成功完成合成的次数
        int craftCount = 0;
        // 记录循环次数用来在游戏可能进入死循环时抛出异常
        int loopCount = 0;
        while (true) {
            // 检查循环次数，在循环次数过多时抛出异常
            loopCount++;
            if (loopCount > FakePlayerUtils.MAX_LOOP_COUNT) {
                throw new InfiniteLoopException();
            }
            // 定义变量记录找到正确合成材料的次数
            int successCount = 0;
            // 依次获取每一个合成材料和遍历合成格
            for (int index = 1; index <= 9; index++) {
                //依次获取每一个合成材料
                ItemStackPredicate predicate = this.predicates[index - 1];
                Slot slot = craftingScreenHandler.getSlot(index);
                // 如果合成格的指定槽位不是所需要合成材料，则丢出该物品
                if (slot.hasStack()) {
                    ItemStack itemStack = slot.getStack();
                    if (predicate.test(itemStack)) {
                        // 合成表格上已经有正确的合成材料，找到正确的合成材料次数自增
                        successCount++;
                    } else {
                        FakePlayerUtils.throwItem(craftingScreenHandler, index, this.fakePlayer);
                    }
                } else {
                    // 如果指定合成材料是空气，则不需要遍历物品栏，直接跳过该物品，并增加找到正确合成材料的次数
                    if (predicate.isEmpty()) {
                        successCount++;
                        continue;
                    }
                    // 遍历物品栏找到需要的合成材料
                    int size = craftingScreenHandler.slots.size();
                    for (int inventoryIndex = 10; inventoryIndex < size; inventoryIndex++) {
                        ItemStack itemStack = craftingScreenHandler.getSlot(inventoryIndex).getStack();
                        if (predicate.test(itemStack)) {
                            // 光标拾取和移动物品
                            if (FakePlayerUtils.withKeepPickupAndMoveItemStack(craftingScreenHandler,
                                    inventoryIndex, index, this.fakePlayer)) {
                                // 找到正确合成材料的次数自增
                                successCount++;
                                break;
                            }
                        } else if (CarpetOrgAdditionSettings.fakePlayerCraftPickItemFromShulkerBox
                                && InventoryUtils.isShulkerBoxItem(itemStack)) {
                            ItemStack contentItemStack = InventoryUtils.pickItemFromShulkerBox(itemStack, predicate);
                            if (!contentItemStack.isEmpty()) {
                                // 丢弃光标上的物品（如果有）
                                FakePlayerUtils.dropCursorStack(craftingScreenHandler, fakePlayer);
                                // 将光标上的物品设置为从潜影盒中取出来的物品
                                craftingScreenHandler.setCursorStack(contentItemStack);
                                // 将光标上的物品放在合成方格的槽位上
                                FakePlayerUtils.pickupCursorStack(craftingScreenHandler, index, fakePlayer);
                                successCount++;
                                break;
                            }
                        }
                        // 合成格没有遍历完毕，继续查找下一个合成材料
                        // 合成格遍历完毕，并且物品栏找不到需要的合成材料，结束方法
                        if (index == 9 && inventoryIndex == size - 1) {
                            return;
                        }
                    }
                }
            }
            // 正确材料找到的次数等于9说明全部找到，可以合成
            if (successCount == 9) {
                // 工作台输出槽里有物品，说明配方正确并且前面的合成没有问题，可以取出合成的物品
                if (craftingScreenHandler.getSlot(0).hasStack()) {
                    FakePlayerUtils.collectItem(craftingScreenHandler, 0, inventory, this.fakePlayer);
                    // 合成成功，合成计数器自增
                    craftCount++;
                    // 避免在一个游戏刻内合成太多物品造成巨量卡顿
                    if (FakePlayerUtils.shouldStop(craftCount)) {
                        return;
                    }
                } else {
                    // 如果没有输出物品，说明之前的合成步骤有误，停止合成
                    FakePlayerUtils.stopCraftAction(this.fakePlayer.getCommandSource(), this.fakePlayer);
                    return;
                }
            } else {
                if (successCount > 9) {
                    // 找到正确合成材料的次数不应该大于合成槽位数量，如果超过了说明前面的操作出了问题，抛出异常结束方法
                    throw new IllegalStateException(this.fakePlayer.getName().getString() + "找到正确合成材料的次数为"
                            + successCount + "，正常不应该超过9");
                }
                // 遍历完物品栏后，如果找到正确合成材料小于9，认为玩家身上没有足够的合成材料了，直接结束方法
                return;
            }
        }
    }


    @Override
    public ArrayList<MutableText> info() {
        // 创建一个集合用来存储可变文本对象，这个集合用来在聊天栏输出多行聊天信息，集合中的每个元素单独占一行
        ArrayList<MutableText> list = new ArrayList<>();
        // 将可变文本“<玩家>正在合成物品，配方:”添加到集合
        ItemStack craftOutput = ItemStackPredicate.getCraftOutput(this.predicates, 3, this.fakePlayer);
        // 如果可以合成物品，返回合成的结果物品，否则返回固定文本“物品”
        Text itemText = craftOutput.isEmpty() ? TextBuilder.translate("carpet.command.item.item") : craftOutput.getItem().getName();
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.craft.result", this.fakePlayer.getDisplayName(), itemText));
        this.addCraftRecipe(list, craftOutput);
        // 判断假玩家是否打开了一个工作台
        if (this.fakePlayer.currentScreenHandler instanceof CraftingScreenHandler currentScreenHandler) {
            // 将可变文本“<玩家>当前合成物品的状态:”添加到集合中
            list.add(TextBuilder.translate("carpet.commands.playerAction.info.craft.state", this.fakePlayer.getDisplayName()));
            // 如果打开了，将每一个合成槽位（包括输出槽位）中的物品的名称和堆叠数组装成一个可变文本对象并添加到集合
            addCraftGridState(currentScreenHandler, list);
        } else {
            // 如果没有打开工作台，将未打开工作台的信息添加到集合
            list.add(
                    TextBuilder.translate(
                            "carpet.commands.playerAction.info.craft.no_crafting_table",
                            this.fakePlayer.getDisplayName(), Items.CRAFTING_TABLE.getName()
                    ));
        }
        return list;
    }

    private void addCraftRecipe(ArrayList<MutableText> list, ItemStack craftOutput) {
        // 配方第一排
        list.add(
                TextBuilder.combineAll(
                        TextProvider.INDENT_SYMBOL,
                        this.predicates[0].getInitialUpperCase(),
                        " ",
                        this.predicates[1].getInitialUpperCase(),
                        " ",
                        this.predicates[2].getInitialUpperCase()
                )
        );
        // 配方第二排
        list.add(
                TextBuilder.combineAll(
                        TextProvider.INDENT_SYMBOL,
                        this.predicates[3].getInitialUpperCase(),
                        " ",
                        this.predicates[4].getInitialUpperCase(),
                        " ",
                        this.predicates[5].getInitialUpperCase(),
                        craftOutput.isEmpty() ? null : TextBuilder.combineAll(" -> ", FakePlayerUtils.getWithCountHoverText(craftOutput))
                )
        );
        // 配方第三排
        list.add(
                TextBuilder.combineAll(
                        TextProvider.INDENT_SYMBOL,
                        this.predicates[6].getInitialUpperCase(),
                        " ",
                        this.predicates[7].getInitialUpperCase(),
                        " ",
                        this.predicates[8].getInitialUpperCase()
                )
        );
    }

    // 添加当前合成方格的状态
    private void addCraftGridState(CraftingScreenHandler currentScreenHandler, ArrayList<MutableText> list) {
        // 合成格第一排
        list.add(TextBuilder.combineAll(
                "    ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(1).getStack()),
                " ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(2).getStack()),
                " ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(3).getStack())
        ));
        // 合成格第二排和输出槽
        list.add(TextBuilder.combineAll(
                "    ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(4).getStack()),
                " ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(5).getStack()),
                " ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(6).getStack()),
                " -> ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(0).getStack())
        ));
        // 合成格第三排
        list.add(TextBuilder.combineAll(
                "    ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(7).getStack()),
                " ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(8).getStack()),
                " ", FakePlayerUtils.getWithCountHoverText(currentScreenHandler.getSlot(9).getStack())
        ));
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (int i = 0; i < predicates.length; i++) {
            json.addProperty(String.valueOf(i), predicates[i].toString());
        }
        return json;
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.crafting_table_craft");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.CRAFTING_TABLE_CRAFT;
    }
}
