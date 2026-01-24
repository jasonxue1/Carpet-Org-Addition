package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.exception.InfiniteLoopException;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StonecuttingAction extends AbstractPlayerAction {
    /**
     * 要使用切石机切制的物品
     */
    private final Item item;
    /**
     * 切石机内按钮的索引
     */
    private final int button;
    public static final String ITEM = "item";
    public static final String BUTTON = "button";
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("stonecutting");

    public StonecuttingAction(EntityPlayerMPFake fakePlayer, Item item, int button) {
        super(fakePlayer);
        this.item = item;
        this.button = button;
    }

    @Override
    protected void tick() {
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
            this.getFakePlayer().drop(itemStack, false, true);
        }
    }

    private void stonecutting(AutoGrowInventory inventory) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        if (fakePlayer.containerMenu instanceof StonecutterMenu stonecutterScreenHandler) {
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
                if (inputSlot.hasItem()) {
                    // 如果有物品，并且是指定物品，设置不需要遍历物品栏
                    ItemStack itemStack = inputSlot.getItem();
                    if (itemStack.is(this.item)) {
                        needToTraverseInventory = false;
                    } else {
                        // 如果不是指定物品，丢出该物品
                        FakePlayerUtils.throwItem(stonecutterScreenHandler, 0, fakePlayer);
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
                stonecutterScreenHandler.clickMenuButton(fakePlayer, this.button);
                // 获取切石机输出槽对象
                Slot outputSlot = stonecutterScreenHandler.getSlot(1);
                // 如果输出槽有物品
                if (outputSlot.hasItem()) {
                    // 收集产物
                    FakePlayerUtils.collectItem(stonecutterScreenHandler, 1, inventory, fakePlayer);
                    craftCount++;
                    // 限制每个游戏刻合成次数
                    int ruleValue = CarpetOrgAdditionSettings.fakePlayerMaxItemOperationCount.get();
                    if (ruleValue > 0 && craftCount >= CarpetOrgAdditionSettings.fakePlayerMaxItemOperationCount.get()) {
                        return;
                    }
                } else {
                    // 否则，认为前面的操作有误，停止合成，结束方法
                    this.stop();
                    MinecraftServer server = ServerUtils.getServer(fakePlayer);
                    MessageUtils.sendMessage(server, KEY.then("error").translate(fakePlayer.getDisplayName(), this.getDisplayName()));
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
    private boolean tryMoveItem(StonecutterMenu screenHandler, Item item) {
        for (int index = 2; index < screenHandler.slots.size(); index++) {
            // 如果找到，移动到切石机输入槽，然后结束循环
            ItemStack itemStack = screenHandler.getSlot(index).getItem();
            if (itemStack.is(item)) {
                if (FakePlayerUtils.withKeepPickupAndMoveItemStack(screenHandler, index, 0, this.getFakePlayer())) {
                    return false;
                }
            } else if (CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.get() && InventoryUtils.isOperableSulkerBox(itemStack)) {
                // 从潜影盒中查找指定物品
                ItemStack stack = InventoryUtils.pickItemFromShulkerBox(itemStack, content -> content.is(this.item));
                // 未找到指定物品
                if (stack.isEmpty()) {
                    continue;
                }
                // 将物品移动到切石机输入槽
                // 丢弃光标上的物品（如果有）
                FakePlayerUtils.dropCursorStack(screenHandler, this.getFakePlayer());
                // 将光标上的物品设置为从潜影盒中取出来的物品
                screenHandler.setCarried(stack);
                // 将光标上的物品放在切石机输入槽位上
                FakePlayerUtils.pickupCursorStack(screenHandler, 0, this.getFakePlayer());
                return false;
            }
        }
        // 如果遍历完物品栏还没有找到指定物品，认为物品栏中没有该物品，结束方法
        return true;
    }

    @Override
    public List<Component> info() {
        // 创建一个物品栏对象用来获取配方的输出物品
        SingleRecipeInput input = new SingleRecipeInput(this.item.getDefaultInstance());
        // 获取假玩家所在的世界对象
        Level world = ServerUtils.getWorld(this.getFakePlayer());
        ItemStack outputItemStack;
        try {
            // 获取与配方对应的物品
            outputItemStack = this.getRecipeResult(this.getFakePlayer(), world, input);
        } catch (IndexOutOfBoundsException e) {
            // 如果索引越界了，将输出物品设置为空
            outputItemStack = ItemStack.EMPTY;
        }
        // 获取输出物品的名称
        Component itemName;
        if (outputItemStack.isEmpty()) {
            // 如果物品为EMPTY，设置物品名称为空气的物品悬停文本
            itemName = Items.AIR.getDefaultInstance().getDisplayName();
        } else {
            itemName = outputItemStack.getDisplayName();
        }
        ArrayList<Component> list = new ArrayList<>();
        LocalizationKey key = this.getInfoLocalizationKey();
        list.add(key.translate(
                        this.getFakePlayer().getDisplayName(),
                        ServerUtils.getName(Items.STONECUTTER),
                        this.item.getDefaultInstance().getDisplayName(),
                        itemName
                )
        );
        if (getFakePlayer().containerMenu instanceof StonecutterMenu stonecutterScreenHandler) {
            // 将按钮索引的信息添加到集合，按钮在之前减去了1，这里再加回来
            list.add(key.then("button").translate(this.button + 1));
            // 将切石机当前的状态的信息添加到集合
            list.add(TextBuilder.combineAll("    ",
                    FakePlayerUtils.getWithCountHoverText(stonecutterScreenHandler.getSlot(0).getItem()), " -> ",
                    FakePlayerUtils.getWithCountHoverText(stonecutterScreenHandler.getSlot(1).getItem())));
        } else {
            // 将假玩家没有打开切石机的消息添加到集合
            list.add(key.then("no_stonecutter").translate(this.getFakePlayer().getDisplayName(), ServerUtils.getName(Items.STONECUTTER)));
        }
        return list;
    }

    // 获取切石机配方输出
    private ItemStack getRecipeResult(EntityPlayerMPFake fakePlayer, Level world, SingleRecipeInput input) {
        for (SelectableRecipe.SingleInputEntry<StonecutterRecipe> entry : world.recipeAccess().stonecutterRecipes().entries()) {
            Optional<RecipeHolder<StonecutterRecipe>> optional = entry.recipe().recipe();
            if (optional.isEmpty()) {
                continue;
            }
            StonecutterRecipe recipe = optional.get().value();
            if (recipe.matches(input, fakePlayer.level())) {
                return recipe.assemble(input);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(ITEM, BuiltInRegistries.ITEM.getKey(this.item).toString());
        json.addProperty(BUTTON, this.button);
        return json;
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.STONECUTTING;
    }
}
