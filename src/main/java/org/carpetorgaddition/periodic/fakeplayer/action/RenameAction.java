package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.ArrayList;

public class RenameAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    public static final String NEW_NAME = "new_name";
    /**
     * 要进行重命名的物品
     */
    private final Item item;
    /**
     * 物品的新名称
     */
    private final String newName;

    public RenameAction(EntityPlayerMPFake fakePlayer, Item item, String newName) {
        super(fakePlayer);
        this.item = item;
        this.newName = newName;
    }

    @Override
    protected void tick() {
        // 如果假玩家对铁砧持续按住右键，就会一直打开新的铁砧界面，同时旧的铁砧界面会自动关闭，关闭旧的铁砧界面时，铁砧内的物品会回到玩家物品栏
        if (getFakePlayer().currentScreenHandler instanceof AnvilScreenHandler anvilScreenHandler) {
            // 如果假玩家没有足够的经验，直接介绍方法，创造玩家给物品重命名不需要消耗经验
            if (getFakePlayer().experienceLevel < 1 && !getFakePlayer().isCreative()) {
                FakePlayerUtils.stopAction(getFakePlayer().getCommandSource(), getFakePlayer(),
                        "carpet.commands.playerAction.rename.not_experience");
                return;
            }
            Slot oneSlot = anvilScreenHandler.getSlot(0);
            // 第一个槽位的物品是否正确：是指定物品，没有被正确重命名，已经最大堆叠
            boolean oneSlotCorrect = false;
            // 判断第一个槽位是否有物品
            if (oneSlot.hasStack()) {
                ItemStack itemStack = oneSlot.getStack();
                // 判断该槽位的物品是否已经正确重命名
                if (itemStack.getName().getString().equals(newName) || !itemStack.isOf(item)) {
                    // 如果已经重命名，或者当前槽位不是指定物品，丢出该槽位的物品
                    // 因为该槽位的物品被丢弃，所以该槽位已经没有物品，没有必要继续判断，直接结束方法
                    FakePlayerUtils.pickupAndThrow(anvilScreenHandler, 0, getFakePlayer());
                    return;
                } else {
                    // 判断当前物品堆栈对象是否为指定物品
                    // 让物品最大堆叠后才能重命名，节省经验
                    if (itemStack.getCount() == itemStack.getMaxCount()) {
                        oneSlotCorrect = true;
                    }
                }
            }
            // 遍历玩家物品栏，找到指定需要重命名的物品
            // 第一个槽位的物品必须是正确的
            for (int index = 3; !oneSlotCorrect && index < anvilScreenHandler.slots.size(); index++) {
                // 这里遍历的是玩家物品栏
                if (anvilScreenHandler.getSlot(index).hasStack()
                        && anvilScreenHandler.getSlot(index).getStack().isOf(item)) {
                    // 找到指定物品后，模拟按住Shift键将物品移动到铁砧输入槽，然后跳出for循环
                    FakePlayerUtils.quickMove(anvilScreenHandler, index, getFakePlayer());
                    break;
                }
                // 如果遍历完物品栏还是没有找到指定物品，认为玩家物品栏中已经没有指定物品，结束方法
                if (index == anvilScreenHandler.slots.size() - 1) {
                    return;
                }
            }
            // 获取铁砧第二个输入槽
            Slot twoSlot = anvilScreenHandler.getSlot(1);
            // 判断该槽位是否有物品
            if (twoSlot.hasStack()) {
                // 如果有，移动到物品栏，如果不能移动，直接丢出
                FakePlayerUtils.quickMove(anvilScreenHandler, 1, getFakePlayer());
                if (twoSlot.hasStack()) {
                    FakePlayerUtils.pickupAndThrow(anvilScreenHandler, 1, getFakePlayer());
                }
            }
            // 判断第一个输入槽是否正确，第二个格子是否没有物品
            if (oneSlotCorrect && !twoSlot.hasStack()) {
                // 设置物品名称
                anvilScreenHandler.setNewItemName(newName);
                // 判断是否可以取出输出槽的物品
                if (anvilScreenHandler.getSlot(2).hasStack() && canTakeOutput(anvilScreenHandler)) {
                    // 丢出输出槽的物品
                    FakePlayerUtils.pickupAndThrow(anvilScreenHandler, 2, getFakePlayer());
                } else {
                    // 如果不能取出，可能玩家已经没有经验，停止重命名
                    FakePlayerUtils.stopAction(getFakePlayer().getCommandSource(), getFakePlayer(),
                            "carpet.commands.playerAction.rename");
                }
            }
        }
    }

    // 判断是否可以输出物品
    private boolean canTakeOutput(AnvilScreenHandler screenHandler) {
        if (this.getFakePlayer().getAbilities().creativeMode || this.getFakePlayer().experienceLevel >= screenHandler.getLevelCost()) {
            return screenHandler.getLevelCost() > 0;
        }
        return false;
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        // 获取假玩家的显示名称
        Text playerName = getFakePlayer().getDisplayName();
        // 将假玩家要重命名的物品和物品新名称的信息添加到集合
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.rename.item",
                playerName, this.item.getDefaultStack().toHoverableText(), newName));
        // 将假玩家剩余经验的信息添加到集合
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.rename.xp",
                getFakePlayer().experienceLevel));
        if (getFakePlayer().currentScreenHandler instanceof AnvilScreenHandler anvilScreenHandler) {
            // 将铁砧GUI上的物品信息添加到集合
            list.add(TextBuilder.combineAll("    ",
                    FakePlayerUtils.getWithCountHoverText(anvilScreenHandler.getSlot(0).getStack()), " ",
                    FakePlayerUtils.getWithCountHoverText(anvilScreenHandler.getSlot(1).getStack()), " -> ",
                    FakePlayerUtils.getWithCountHoverText(anvilScreenHandler.getSlot(2).getStack())));
        } else {
            // 将假玩家没有打开铁砧的信息添加到集合
            list.add(TextBuilder.translate("carpet.commands.playerAction.info.rename.no_anvil",
                    playerName, Items.ANVIL.getName()));
        }
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(ITEM, Registries.ITEM.getId(item).toString());
        json.addProperty(NEW_NAME, this.newName);
        return json;
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.rename");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.RENAME;
    }
}
