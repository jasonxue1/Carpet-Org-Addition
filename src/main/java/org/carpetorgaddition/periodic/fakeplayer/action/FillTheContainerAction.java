package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.predicate.ItemStackPredicate;

import java.util.ArrayList;
import java.util.List;
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
    protected void tick() {
        AbstractContainerMenu screenHandler = getFakePlayer().containerMenu;
        if (screenHandler instanceof InventoryMenu) {
            return;
        }
        // 获取要装在潜影盒的物品
        for (int index : getRange(screenHandler)) {
            Slot slot = screenHandler.getSlot(index);
            ItemStack itemStack = slot.getItem();
            if (itemStack.isEmpty()) {
                continue;
            }
            if (this.predicate.test(itemStack)) {
                if (screenHandler instanceof ShulkerBoxMenu && !itemStack.getItem().canFitInsideContainerItems()) {
                    // 丢弃不能放入潜影盒的物品
                    if (this.dropOther) {
                        FakePlayerUtils.throwItem(screenHandler, index, this.getFakePlayer());
                    }
                    continue;
                }
                // 模拟按住Shift键移动物品
                if (FakePlayerUtils.quickMove(screenHandler, index, this.getFakePlayer()).isEmpty()) {
                    this.getFakePlayer().doCloseContainer();
                    return;
                }
            } else if (this.dropOther) {
                FakePlayerUtils.throwItem(screenHandler, index, this.getFakePlayer());
            }
        }
    }

    private Integer[] getRange(AbstractContainerMenu screenHandler) {
        IntStream intStream;
        if (this.moreContainer) {
            intStream = switch (screenHandler) {
                case ShulkerBoxMenu ignored -> IntStream.rangeClosed(27, 62);
                // 箱子，末影箱，木桶等容器
                case ChestMenu handler
                        when handler.getType() == MenuType.GENERIC_9x3 -> IntStream.rangeClosed(27, 62);
                // 大箱子，GCA假人背包
                case ChestMenu handler
                        when handler.getType() == MenuType.GENERIC_9x6 -> IntStream.rangeClosed(54, 89);
                // 漏斗
                case HopperMenu ignored -> IntStream.rangeClosed(5, 40);
                // 发射器，投掷器
                case DispenserMenu ignored -> IntStream.rangeClosed(9, 44);
                // 合成器
                case CrafterMenu ignored -> IntStream.rangeClosed(9, 44);
                case null, default -> IntStream.of();
            };
        } else {
            if (screenHandler instanceof ShulkerBoxMenu) {
                intStream = IntStream.rangeClosed(27, 62);
            } else {
                intStream = IntStream.of();
            }
        }
        return intStream.boxed().toArray(Integer[]::new);
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        Component translate = TextBuilder.translate(
                "carpet.commands.playerAction.info.fill.predicate",
                this.getFakePlayer().getDisplayName(),
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
    public Component getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.fill");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.FILL_THE_CONTAINER;
    }
}
