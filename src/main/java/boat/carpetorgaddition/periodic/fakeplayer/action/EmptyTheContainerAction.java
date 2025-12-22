package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EmptyTheContainerAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    private final ItemStackPredicate predicate;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("clean");

    public EmptyTheContainerAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate predicate) {
        super(fakePlayer);
        this.predicate = predicate;
    }

    @Override
    protected void tick() {
        AbstractContainerMenu screenHandler = getFakePlayer().containerMenu;
        if (screenHandler instanceof InventoryMenu) {
            return;
        }
        for (int index = 0; index < screenHandler.slots.size(); index++) {
            // 如果遍历到了玩家物品栏槽位，直接结束循环，因为后面一般不会再有容器槽位了
            // 合成器的输出槽位虽然在玩家物品栏槽位后面，但是这个槽位的物品无法取出，因此可以忽略
            if (screenHandler.getSlot(index).container instanceof Inventory) {
                break;
            }
            ItemStack itemStack = screenHandler.getSlot(index).getItem();
            if (itemStack.isEmpty() || InventoryUtils.isGcaItem(itemStack)) {
                continue;
            }
            if (this.predicate.test(itemStack)) {
                // 丢弃一组物品
                FakePlayerUtils.throwItem(screenHandler, index, getFakePlayer());
            }
        }
        // 物品全部丢出后自动关闭容器
        getFakePlayer().closeContainer();
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(ITEM, this.predicate.toString());
        return json;
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        Component text = this.predicate.getDisplayName();
        Component playerName = this.getFakePlayer().getDisplayName();
        list.add(this.getInfoLocalizationKey().translate(playerName, text));
        return list;
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.EMPTY_THE_CONTAINER;
    }
}
