package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.AutoGrowInventory;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.ArrayList;

public class InventoryCraftingAction extends AbstractPlayerAction {
    /**
     * 物品合成所使用的物品栏
     */
    private final ItemStackPredicate[] predicates = new ItemStackPredicate[4];

    public InventoryCraftingAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate[] predicates) {
        super(fakePlayer);
        if (predicates.length != 4) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(predicates, 0, this.predicates, 0, this.predicates.length);
    }

    @Override
    public void tick() {
        AutoGrowInventory inventory = new AutoGrowInventory();
        this.inventoryCraft(inventory);
        // 丢弃合成输出
        for (ItemStack itemStack : inventory) {
            this.fakePlayer.dropItem(itemStack, false, true);
        }
    }


    private void inventoryCraft(AutoGrowInventory inventory) {
        PlayerScreenHandler playerScreenHandler = fakePlayer.playerScreenHandler;
        // 定义变量记录成功完成合成的次数
        int craftCount = 0;
        // 记录循环次数用来在游戏可能进入死循环时抛出异常
        int loopCount = 0;
        while (true) {
            // 检查循环次数
            loopCount++;
            if (loopCount > FakePlayerUtils.MAX_LOOP_COUNT) {
                throw new InfiniteLoopException();
            }
            // 定义变量记录找到正确合成材料的次数
            int successCount = 0;
            // 遍历4x4合成格
            for (int craftIndex = 1; craftIndex <= 4; craftIndex++) {
                // 获取每一个合成材料
                ItemStackPredicate matcher = this.predicates[craftIndex - 1];
                Slot slot = playerScreenHandler.getSlot(craftIndex);
                // 检查合成格上是否已经有物品
                if (slot.hasStack()) {
                    // 如果有并且物品是正确的合成材料，直接结束本轮循环，即跳过该物品
                    if (matcher.test(slot.getStack())) {
                        successCount++;
                        continue;
                    } else {
                        // 如果不是，丢出该物品
                        FakePlayerUtils.throwItem(playerScreenHandler, craftIndex, fakePlayer);
                    }
                } else if (matcher.isEmpty()) {
                    successCount++;
                    continue;
                }
                int size = playerScreenHandler.slots.size();
                // 遍历物品栏，包括盔甲槽和副手槽
                for (int inventoryIndex = 5; inventoryIndex < size; inventoryIndex++) {
                    ItemStack itemStack = playerScreenHandler.getSlot(inventoryIndex).getStack();
                    // 如果该槽位是正确的合成材料，将该物品移动到合成格，然后增加找到正确合成材料的次数
                    if (matcher.test(itemStack)) {
                        if (FakePlayerUtils.withKeepPickupAndMoveItemStack(playerScreenHandler,
                                inventoryIndex, craftIndex, this.fakePlayer)) {
                            successCount++;
                            break;
                        }
                    } else if (CarpetOrgAdditionSettings.fakePlayerCraftPickItemFromShulkerBox
                            && InventoryUtils.isShulkerBoxItem(itemStack)) {
                        ItemStack contentItemStack = InventoryUtils.pickItemFromShulkerBox(itemStack, matcher);
                        if (!contentItemStack.isEmpty()) {
                            // 丢弃光标上的物品（如果有）
                            FakePlayerUtils.dropCursorStack(playerScreenHandler, this.fakePlayer);
                            // 将光标上的物品设置为从潜影盒中取出来的物品
                            playerScreenHandler.setCursorStack(contentItemStack);
                            // 将光标上的物品放在合成方格的槽位上
                            FakePlayerUtils.pickupCursorStack(playerScreenHandler, craftIndex, this.fakePlayer);
                            successCount++;
                            break;
                        }
                    }
                    // 如果遍历完物品栏还没有找到指定物品，认为玩家身上已经没有该物品，结束方法
                    if (craftIndex == 4 && inventoryIndex == size - 1) {
                        return;
                    }
                }
            }
            // 如果找到正确合成材料的次数为4，认为找到了所有的合成材料，尝试输出物品
            if (successCount == 4) {
                // 如果输出槽有物品，则丢出该物品
                if (playerScreenHandler.getSlot(0).hasStack()) {
                    FakePlayerUtils.collectItem(playerScreenHandler, 0, inventory, this.fakePlayer);
                    // 合成成功，合成计数器自增
                    craftCount++;
                    // 避免在一个游戏刻内合成太多物品造成巨量卡顿
                    if (FakePlayerUtils.shouldStop(craftCount)) {
                        return;
                    }
                } else {
                    // 如果输出槽没有物品，认为前面的合成操作有误，停止合成
                    FakePlayerUtils.stopCraftAction(this.fakePlayer.getCommandSource(), this.fakePlayer);
                    return;
                }
            } else {
                if (successCount > 4) {
                    // 找到正确合成材料的次数不应该大于合成槽位数量，如果超过了说明前面的操作出了问题，抛出异常结束方法
                    throw new IllegalStateException(this.fakePlayer.getName().getString() + "找到正确合成材料的次数为"
                            + successCount + "，正常不应该超过4");
                }
                // 遍历完物品栏后，如果没有找到足够多的合成材料，认为玩家身上没有足够的合成材料了，直接结束方法
                return;
            }
        }
    }


    @Override
    public ArrayList<MutableText> info() {
        // 创建一个集合用来存储可变文本对象，这个集合用来在聊天栏输出多行聊天信息，集合中的每个元素单独占一行
        ArrayList<MutableText> list = new ArrayList<>();
        // 获取假玩家的显示名称
        Text playerName = this.fakePlayer.getDisplayName();
        // 将可变文本“<玩家>正在合成物品，配方:”添加到集合
        ItemStack craftOutput = ItemStackPredicate.getCraftOutput(this.predicates, 2, this.fakePlayer);
        // 如果可以合成物品，返回合成的结果物品，否则返回固定文本“物品”
        Text itemText = craftOutput.isEmpty() ? TextUtils.translate("carpet.command.item.item") : craftOutput.getItem().getName();
        list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.result", playerName, itemText));
        this.addCraftRecipe(list, craftOutput);
        // 将可变文本“<玩家>当前合成物品的状态:”添加到集合中
        list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.state", playerName));
        // 获取玩家的生存模式物品栏对象
        PlayerScreenHandler playerScreenHandler = this.fakePlayer.playerScreenHandler;
        // 将每一个合成槽位（包括输出槽位）中的物品的名称和堆叠数组装成一个可变文本对象并添加到集合
        addCraftGridState(list, playerScreenHandler);
        return list;
    }

    // 添加合成配方文本
    private void addCraftRecipe(ArrayList<MutableText> list, ItemStack craftOutput) {
        // 配方第一排
        list.add(new TextBuilder()
                .indentation().append(this.predicates[0].getInitialUpperCase())
                .blank().append(this.predicates[1].getInitialUpperCase())
                .toLine());
        // 配方第二排
        TextBuilder builder = new TextBuilder()
                .indentation().append(this.predicates[2].getInitialUpperCase())
                .blank().append(this.predicates[3].getInitialUpperCase());
        if (!craftOutput.isEmpty()) {
            builder.appendString(" -> ").append(FakePlayerUtils.getWithCountHoverText(craftOutput));
        }
        list.add(builder.toLine());
    }

    // 合成方格内的物品状态
    private void addCraftGridState(ArrayList<MutableText> list, PlayerScreenHandler playerScreenHandler) {
        // 合成格第一排
        list.add(TextUtils.appendAll(
                "    ", FakePlayerUtils.getWithCountHoverText(playerScreenHandler.getSlot(1).getStack()),
                " ", FakePlayerUtils.getWithCountHoverText(playerScreenHandler.getSlot(2).getStack())
        ));
        // 合成格第二排和输出槽
        list.add(TextUtils.appendAll(
                "    ", FakePlayerUtils.getWithCountHoverText(playerScreenHandler.getSlot(3).getStack()),
                " ", FakePlayerUtils.getWithCountHoverText(playerScreenHandler.getSlot(4).getStack()),
                " -> ", FakePlayerUtils.getWithCountHoverText(playerScreenHandler.getSlot(0).getStack())
        ));
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (int i = 0; i < this.predicates.length; i++) {
            json.addProperty(String.valueOf(i), this.predicates[i].toString());
        }
        return json;
    }

    @Override
    public MutableText getDisplayName() {
        return TextUtils.translate("carpet.commands.playerAction.action.inventory_craft");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.INVENTORY_CRAFTING;
    }
}
