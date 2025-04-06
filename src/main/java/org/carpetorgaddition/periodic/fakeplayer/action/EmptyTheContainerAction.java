package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;

import java.util.ArrayList;

public class EmptyTheContainerAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    private final ItemStackPredicate predicate;

    public EmptyTheContainerAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate predicate) {
        super(fakePlayer);
        this.predicate = predicate;
    }

    @Override
    public void tick() {
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
            if (this.predicate.test(itemStack)) {
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
        json.addProperty(ITEM, this.predicate.toString());
        return json;
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        Text text = this.predicate.toText();
        Text playerName = this.fakePlayer.getDisplayName();
        list.add(TextUtils.translate("carpet.commands.playerAction.info.clean.predicate", playerName, text));
        return list;
    }

    @Override
    public MutableText getDisplayName() {
        return TextUtils.translate("carpet.commands.playerAction.action.clean");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.EMPTY_THE_CONTAINER;
    }
}
