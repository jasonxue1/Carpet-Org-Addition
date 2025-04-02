package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.TextUtils;

import java.util.ArrayList;

public class CleanContainerAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    public static final String ALL_ITEM = "allItem";
    /**
     * 要从潜影盒中丢出的物品
     */
    private final Item item;
    /**
     * 是否忽略{@link CleanContainerAction#item}，并清空潜影盒内的所有物品
     */
    private final boolean allItem;

    public CleanContainerAction(EntityPlayerMPFake fakePlayer, Item item, boolean allItem) {
        super(fakePlayer);
        this.item = item;
        this.allItem = allItem;
    }

    @Override
    public void tick() {
        Item item = this.allItem ? null : this.item;
        ScreenHandler screenHandler = fakePlayer.currentScreenHandler;
        if (screenHandler == null || screenHandler instanceof PlayerScreenHandler) {
            return;
        }
        for (int index = 0; index < screenHandler.slots.size(); index++) {
            // 如果遍历到了玩家物品栏槽位，直接结束循环，因为后面一般不会再有容器槽位了
            // 合成器的输出槽位虽然在玩家物品栏槽位后面，但是这个槽位的物品无法取出，因此可以忽略
            if (screenHandler.getSlot(index).inventory instanceof PlayerInventory) {
                break;
            }
            ItemStack itemStack = screenHandler.getSlot(index).getStack();
            if (itemStack.isEmpty() || FakePlayerUtils.isGcaItem(itemStack)) {
                continue;
            }
            if (this.allItem || itemStack.isOf(item)) {
                // 丢弃一组物品
                FakePlayerUtils.throwItem(screenHandler, index, fakePlayer);
            }
        }
        // 物品全部丢出后自动关闭容器
        fakePlayer.closeHandledScreen();
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.item != null) {
            // 要清空的物品
            json.addProperty(ITEM, Registries.ITEM.getId(this.item).toString());
        }
        json.addProperty(ALL_ITEM, this.allItem);
        return json;
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        if (this.allItem) {
            // 将玩家清空潜影盒的信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.clean.item",
                    this.fakePlayer.getDisplayName(),
                    Items.SHULKER_BOX.getName()));
        } else {
            // 将玩家清空潜影盒的信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.clean.designated_item",
                    this.fakePlayer.getDisplayName(),
                    Items.SHULKER_BOX.getName(),
                    this.item.getName()));
        }
        return list;
    }

    @Override
    public MutableText getDisplayName() {
        return TextUtils.translate("carpet.commands.playerAction.action.clean");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.CLEAN;
    }
}
