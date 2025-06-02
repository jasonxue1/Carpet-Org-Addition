package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class FillTheContainerAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    public static final String DROP_OTHER = "dropOther";
    public static final String MORE_CONTAINER = "moreContainer";
    /**
     * 要向容器填充的物品
     */
    private final ItemStackPredicate predicate;
    private final boolean dropOther;
    private final boolean moreContainer;

    public FillTheContainerAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate predicate, boolean dropOther, boolean moreContainer) {
        super(fakePlayer);
        this.predicate = predicate;
        this.dropOther = dropOther;
        this.moreContainer = moreContainer;
    }

    @Override
    public void tick() {
        ScreenHandler screenHandler = fakePlayer.currentScreenHandler;
        if (screenHandler == null || screenHandler instanceof PlayerScreenHandler) {
            return;
        }
        // 获取要装在潜影盒的物品
        for (int index : getRange(screenHandler)) {
            Slot slot = screenHandler.getSlot(index);
            ItemStack itemStack = slot.getStack();
            if (itemStack.isEmpty()) {
                continue;
            }
            if (this.predicate.test(itemStack)) {
                if (screenHandler instanceof ShulkerBoxScreenHandler && !itemStack.getItem().canBeNested()) {
                    // 丢弃不能放入潜影盒的物品
                    if (this.dropOther) {
                        FakePlayerUtils.throwItem(screenHandler, index, this.fakePlayer);
                    }
                    continue;
                }
                // 模拟按住Shift键移动物品
                if (FakePlayerUtils.quickMove(screenHandler, index, this.fakePlayer).isEmpty()) {
                    this.fakePlayer.onHandledScreenClosed();
                    return;
                }
            } else if (this.dropOther) {
                FakePlayerUtils.throwItem(screenHandler, index, this.fakePlayer);
            }
        }
    }

    private Integer[] getRange(ScreenHandler screenHandler) {
        IntStream intStream;
        if (this.moreContainer) {
            intStream = switch (screenHandler) {
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
        } else {
            if (screenHandler instanceof ShulkerBoxScreenHandler) {
                intStream = IntStream.rangeClosed(27, 62);
            } else {
                intStream = IntStream.of();
            }
        }
        return intStream.boxed().toArray(Integer[]::new);
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        MutableText translate = TextBuilder.translate(
                "carpet.commands.playerAction.info.fill.predicate",
                this.fakePlayer.getDisplayName(),
                this.predicate.toText()
        );
        list.add(translate);
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        // 要清空的物品
        json.addProperty(ITEM, this.predicate.toString());
        // 是否丢弃其他物品
        json.addProperty(DROP_OTHER, this.dropOther);
        // 是否支持潜影盒以外的其它容器
        json.addProperty(MORE_CONTAINER, this.moreContainer);
        return json;
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.fill");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.FILL_THE_CONTAINER;
    }
}
