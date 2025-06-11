package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import org.apache.commons.lang3.mutable.MutableInt;
import org.carpetorgaddition.mixin.command.FishingBobberEntityAccessor;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.ArrayList;

public class FishingAction extends AbstractPlayerAction {
    private final MutableInt timer = new MutableInt();

    public FishingAction(EntityPlayerMPFake fakePlayer) {
        super(fakePlayer);
    }

    @Override
    protected void tick() {
        // 检查玩家是否持有钓鱼竿
        if (pickFishingRod()) {
            // 检查玩家是否抛出钓竿
            FishingBobberEntity fishHook = this.getFakePlayer().fishHook;
            if (fishHook == null) {
                // 检查计时器是否清零
                if (this.timer.getValue() == 0) {
                    // 右键抛出钓鱼竿
                    this.use();
                } else {
                    this.timer.decrement();
                }
            } else {
                // 如果钓鱼竿钩到方块或其它实体，通过切换物品收杆，防止额外的耐久损耗
                if (fishHook.isOnGround() || fishHook.getHookedEntity() != null) {
                    switchInventory();
                }
                // 检查鱼是否上钩
                if (canReelInTheFishingPole(fishHook)) {
                    // 右键收杆
                    this.use();
                    // 设置5个游戏刻后重新抛竿
                    this.timer.setValue(5);
                }
            }
        }
    }

    /**
     * 将钓鱼竿拿到手上
     *
     * @return 物品栏是否有钓鱼竿
     */
    private boolean pickFishingRod() {
        // 如果玩家手上有钓鱼竿，无需切换
        if (this.getFakePlayer().getMainHandStack().isOf(Items.FISHING_ROD) || this.getFakePlayer().getOffHandStack().isOf(Items.FISHING_ROD)) {
            return true;
        }
        // 从物品栏拿取钓鱼竿
        PlayerInventory inventory = this.getFakePlayer().getInventory();
        for (int i = 0; i < inventory.getMainStacks().size(); i++) {
            if (inventory.getStack(i).isOf(Items.FISHING_ROD)) {
                // 将非钓鱼竿物品放入主手
                inventory.swapSlotWithHotbar(i);
                if (this.getFakePlayer().getMainHandStack().isOf(Items.FISHING_ROD)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 通过切换物品栏收起钓鱼竿
     */
    private void switchInventory() {
        if (this.getFakePlayer().getOffHandStack().isOf(Items.FISHING_ROD)) {
            swapHands();
        }
        PlayerInventory inventory = this.getFakePlayer().getInventory();
        // 查找物品栏内的非钓鱼竿物品
        for (int i = 0; i < inventory.getMainStacks().size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            // 其它钓鱼竿物品不能与主手物品切换
            if (itemStack.isOf(Items.FISHING_ROD)) {
                continue;
            }
            // 将非钓鱼竿物品放入主手
            inventory.swapSlotWithHotbar(i);
            // 检查钓鱼竿是否切换成功（钓鱼竿不能与盔甲槽中的物品切换），如果成功，结束方法
            if (this.getFakePlayer().getMainHandStack().isOf(Items.FISHING_ROD)) {
                // 主手是钓鱼竿，切换失败
                continue;
            }
            return;
        }
    }

    /**
     * @return 是否可以收杆
     */
    private boolean canReelInTheFishingPole(FishingBobberEntity fishHook) {
        return ((FishingBobberEntityAccessor) fishHook).getHookCountdown() > 0;
    }

    /**
     * 右键
     */
    private void use() {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) this.getFakePlayer()).getActionPack();
        actionPack.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.once());
    }

    /**
     * 交换主副手物品
     */
    private void swapHands() {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) this.getFakePlayer()).getActionPack();
        actionPack.start(EntityPlayerActionPack.ActionType.SWAP_HANDS, EntityPlayerActionPack.Action.once());
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.fishing", this.getFakePlayer().getDisplayName()));
        return list;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.fishing");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.FISHING;
    }
}
