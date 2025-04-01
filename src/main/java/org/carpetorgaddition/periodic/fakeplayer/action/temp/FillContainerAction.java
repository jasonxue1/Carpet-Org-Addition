package org.carpetorgaddition.periodic.fakeplayer.action.temp;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.TextUtils;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class FillContainerAction extends AbstractPlayerAction {
    private static final String ITEM = "item";
    public static final String ALL_ITEM = "allItem";
    public static final String DROP_OTHER = "dropOther";
    /**
     * 要向潜影盒填充的物品
     */
    private final Item item;
    /**
     * 是否向潜影盒内填充任意物品并忽略{@link FillContainerAction#item}（本身就不能放入潜影盒的物品不会被填充）
     */
    private final boolean allItem;
    private final boolean dropOther;

    public FillContainerAction(EntityPlayerMPFake fakePlayer, Item item, boolean allItem, boolean dropOther) {
        super(fakePlayer);
        this.item = item;
        this.allItem = allItem;
        this.dropOther = dropOther;
    }

    @Override
    public void tick() {
        ScreenHandler screenHandler = fakePlayer.currentScreenHandler;
        if (screenHandler == null || screenHandler instanceof PlayerScreenHandler) {
            return;
        }
        // 获取要装在潜影盒的物品
        Item item = this.allItem ? null : this.item;
        for (int index : getRange(screenHandler)) {
            Slot slot = screenHandler.getSlot(index);
            ItemStack itemStack = slot.getStack();
            if (itemStack.isEmpty()) {
                continue;
            }
            if ((allItem || itemStack.isOf(item))) {
                if (screenHandler instanceof ShulkerBoxScreenHandler && !itemStack.getItem().canBeNested()) {
                    // 丢弃不能放入潜影盒的物品
                    if (this.dropOther) {
                        FakePlayerUtils.throwItem(screenHandler, index, fakePlayer);
                    }
                    continue;
                }
                // 模拟按住Shift键移动物品
                if (FakePlayerUtils.quickMove(screenHandler, index, fakePlayer).isEmpty()) {
                    fakePlayer.onHandledScreenClosed();
                    return;
                }
            } else if (this.dropOther) {
                FakePlayerUtils.throwItem(screenHandler, index, fakePlayer);
            }
        }
    }

    private Integer[] getRange(ScreenHandler screenHandler) {
        IntStream intStream = switch (screenHandler) {
            case ShulkerBoxScreenHandler ignored -> IntStream.rangeClosed(27, 62);
            // 箱子，末影箱，木桶等容器
            case GenericContainerScreenHandler handler
                    when handler.getType() == ScreenHandlerType.GENERIC_9X3 -> IntStream.rangeClosed(27, 62);
            // 大箱子，GCA假人背包
            case GenericContainerScreenHandler handler
                    when handler.getType() == ScreenHandlerType.GENERIC_9X6 -> IntStream.rangeClosed(54, 89);
            // 漏斗
            case HopperScreenHandler ignored -> IntStream.rangeClosed(5, 40);
            // 发射器，投掷器
            case Generic3x3ContainerScreenHandler ignored -> IntStream.rangeClosed(9, 44);
            // 合成器
            case CrafterScreenHandler ignored -> IntStream.rangeClosed(9, 44);
            case null, default -> IntStream.of();
        };
        return intStream.boxed().toArray(Integer[]::new);
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        if (this.allItem) {
            // 将“<玩家名> 正在向 潜影盒 填充 [item] 物品”信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.fill_all.item",
                    this.fakePlayer.getDisplayName(), Items.SHULKER_BOX.getName()));
        } else {
            // 将“<玩家名> 正在向 潜影盒 填充 [item] 物品”信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.fill.item",
                    this.fakePlayer.getDisplayName(), Items.SHULKER_BOX.getName(), this.item.getDefaultStack().toHoverableText()));
        }
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.item != null) {
            // 要清空的物品
            json.addProperty(ITEM, Registries.ITEM.getId(this.item).toString());
        }
        json.addProperty(ALL_ITEM, this.allItem);
        json.addProperty(DROP_OTHER, this.dropOther);
        return json;
    }

    @Override
    public MutableText getDisplayName() {
        return TextUtils.translate("carpet.commands.playerAction.action.fill");
    }

    @Override
    public String getSerializedName() {
        return "fill";
    }
}
