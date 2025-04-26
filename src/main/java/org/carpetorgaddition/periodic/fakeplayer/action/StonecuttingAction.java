package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.StonecuttingRecipe;
import net.minecraft.recipe.display.CuttingRecipeDisplay;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.AutoGrowInventory;

import java.util.ArrayList;
import java.util.Optional;

public class StonecuttingAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    public static final String BUTTON = "button";
    /**
     * 要使用切石机切制的物品
     */
    private final Item item;
    /**
     * 切石机内按钮的索引
     */
    private final int button;

    public StonecuttingAction(EntityPlayerMPFake fakePlayer, Item item, int button) {
        super(fakePlayer);
        this.item = item;
        this.button = button;
    }

    @Override
    public void tick() {
        /*
         * 切石机的输出槽不能使用Ctrl+Q一次性丢出整组物品，只能一个个丢出。在合成
         * 物品时，会有大量物品产生，因为是一个个丢出的，所以物品不会立即合并。例如，
         * 一组石头合成石砖时，不会生成一整组的石砖，而是生成64个未堆叠的石砖，合成时，
         * 会因为物品实体过多，导致巨量卡顿，甚至游戏卡死。因此，这里加一个物品栏用来
         * 临时保存合成输出的物品，在物品栏内完成物品的合并，并在本tick的合成结束时
         * 一次性丢出物品栏内的所有物品，这样丢出的物品就是已经合并的，并显著减少卡顿
         */
        AutoGrowInventory inventory = new AutoGrowInventory();
        // 合成物品
        this.stonecutting(inventory);
        // 丢弃合成输出
        for (ItemStack itemStack : inventory) {
            this.fakePlayer.dropItem(itemStack, false, true);
        }
    }

    private void stonecutting(AutoGrowInventory inventory) {
        if (fakePlayer.currentScreenHandler instanceof StonecutterScreenHandler stonecutterScreenHandler) {
            // 定义变量记录成功完成合成的次数
            int craftCount = 0;
            // 用于循环次数过多时抛出异常结束循环
            int loopCount = 0;
            while (true) {
                loopCount++;
                if (loopCount > 1000) {
                    throw new InfiniteLoopException();
                }
                // 定义变量记录是否需要遍历物品栏
                boolean needToTraverseInventory = true;
                // 获取切石机输入槽对象
                Slot inputSlot = stonecutterScreenHandler.getSlot(0);
                // 判断切石机输入槽是否有物品
                if (inputSlot.hasStack()) {
                    // 如果有物品，并且是指定物品，设置不需要遍历物品栏
                    ItemStack itemStack = inputSlot.getStack();
                    if (itemStack.isOf(this.item)) {
                        needToTraverseInventory = false;
                    } else {
                        // 如果不是指定物品，丢出该物品
                        FakePlayerUtils.throwItem(stonecutterScreenHandler, 0, this.fakePlayer);
                    }
                }
                // 如果需要遍历物品栏
                if (needToTraverseInventory) {
                    // 尝试从物品栏中找到需要的物品
                    if (this.tryMoveItem(stonecutterScreenHandler, this.item)) {
                        return;
                    }
                }
                // 模拟单击切石机按钮
                stonecutterScreenHandler.onButtonClick(this.fakePlayer, this.button);
                // 获取切石机输出槽对象
                Slot outputSlot = stonecutterScreenHandler.getSlot(1);
                // 如果输出槽有物品
                if (outputSlot.hasStack()) {
                    // 收集产物
                    FakePlayerUtils.collectItem(stonecutterScreenHandler, 1, inventory, this.fakePlayer);
                    craftCount++;
                    // 限制每个游戏刻合成次数
                    int ruleValue = CarpetOrgAdditionSettings.fakePlayerMaxCraftCount;
                    if (ruleValue > 0 && craftCount >= CarpetOrgAdditionSettings.fakePlayerMaxCraftCount) {
                        return;
                    }
                } else {
                    // 否则，认为前面的操作有误，停止合成，结束方法
                    FakePlayerUtils.stopAction(this.fakePlayer.getCommandSource(), this.fakePlayer, "carpet.commands.playerAction.stone_cutting");
                    return;
                }
            }
        }
    }

    /**
     * 将物品移动到切石机输入槽位
     *
     * @return 是否已经所有材料用完
     */
    private boolean tryMoveItem(StonecutterScreenHandler screenHandler, Item item) {
        for (int index = 2; index < screenHandler.slots.size(); index++) {
            // 如果找到，移动到切石机输入槽，然后结束循环
            ItemStack itemStack = screenHandler.getSlot(index).getStack();
            if (itemStack.isOf(item)) {
                if (FakePlayerUtils.withKeepPickupAndMoveItemStack(screenHandler, index, 0, this.fakePlayer)) {
                    return false;
                }
            } else if (CarpetOrgAdditionSettings.fakePlayerCraftPickItemFromShulkerBox && InventoryUtils.isShulkerBoxItem(itemStack)) {
                // 从潜影盒中查找指定物品
                ItemStack stack = InventoryUtils.pickItemFromShulkerBox(itemStack, content -> content.isOf(this.item));
                // 未找到指定物品
                if (stack.isEmpty()) {
                    continue;
                }
                // 将物品移动到切石机输入槽
                // 丢弃光标上的物品（如果有）
                FakePlayerUtils.dropCursorStack(screenHandler, this.fakePlayer);
                // 将光标上的物品设置为从潜影盒中取出来的物品
                screenHandler.setCursorStack(stack);
                // 将光标上的物品放在切石机输入槽位上
                FakePlayerUtils.pickupCursorStack(screenHandler, 0, this.fakePlayer);
                return false;
            }
        }
        // 如果遍历完物品栏还没有找到指定物品，认为物品栏中没有该物品，结束方法
        return true;
    }

    @Override
    public ArrayList<MutableText> info() {
        // 创建一个物品栏对象用来获取配方的输出物品
        SingleStackRecipeInput input = new SingleStackRecipeInput(this.item.getDefaultStack());
        // 获取假玩家所在的世界对象
        World world = this.fakePlayer.getWorld();
        ItemStack outputItemStack;
        try {
            // 获取与配方对应的物品
            outputItemStack = this.getRecipeResult(this.fakePlayer, world, input);
        } catch (IndexOutOfBoundsException e) {
            // 如果索引越界了，将输出物品设置为空
            outputItemStack = ItemStack.EMPTY;
        }
        // 获取输出物品的名称
        Text itemName;
        if (outputItemStack.isEmpty()) {
            // 如果物品为EMPTY，设置物品名称为空气的物品悬停文本
            itemName = Items.AIR.getDefaultStack().toHoverableText();
        } else {
            itemName = outputItemStack.toHoverableText();
        }
        ArrayList<MutableText> list = new ArrayList<>();
        list.add(TextUtils.translate("carpet.commands.playerAction.info.stonecutting.item",
                this.fakePlayer.getDisplayName(), Items.STONECUTTER.getName(),
                this.item.getDefaultStack().toHoverableText(), itemName));
        if (fakePlayer.currentScreenHandler instanceof StonecutterScreenHandler stonecutterScreenHandler) {
            // 将按钮索引的信息添加到集合，按钮在之前减去了1，这里再加回来
            list.add(TextUtils.translate("carpet.commands.playerAction.info.stonecutting.button",
                    (this.button + 1)));
            // 将切石机当前的状态的信息添加到集合
            list.add(TextUtils.appendAll("    ",
                    FakePlayerUtils.getWithCountHoverText(stonecutterScreenHandler.getSlot(0).getStack()), " -> ",
                    FakePlayerUtils.getWithCountHoverText(stonecutterScreenHandler.getSlot(1).getStack())));
        } else {
            // 将假玩家没有打开切石机的消息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.stonecutting.no_stonecutting",
                    this.fakePlayer.getDisplayName(), Items.STONECUTTER.getName()));
        }
        return list;
    }

    // 获取切石机配方输出
    private ItemStack getRecipeResult(EntityPlayerMPFake fakePlayer, World world, SingleStackRecipeInput input) {
        for (CuttingRecipeDisplay.GroupEntry<StonecuttingRecipe> entry : world.getRecipeManager().getStonecutterRecipes().entries()) {
            Optional<RecipeEntry<StonecuttingRecipe>> optional = entry.recipe().recipe();
            if (optional.isEmpty()) {
                continue;
            }
            StonecuttingRecipe recipe = optional.get().value();
            if (recipe.matches(input, fakePlayer.getWorld())) {
                return recipe.craft(input, world.getRegistryManager());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(ITEM, Registries.ITEM.getId(this.item).toString());
        json.addProperty(BUTTON, this.button);
        return json;
    }

    @Override
    public MutableText getDisplayName() {
        return TextUtils.translate("carpet.commands.playerAction.action.stonecutting");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.STONECUTTING;
    }
}
