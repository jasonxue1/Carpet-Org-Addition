package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.ArrayList;
import java.util.List;

public class ItemCategorizeAction extends AbstractPlayerAction {
    public static final String ITEM = "item";
    public static final String THIS_VEC = "thisVec";
    public static final String OTHER_VEC = "otherVec";
    /**
     * 要分拣的物品
     */
    private final ItemStackPredicate predicate;
    /**
     * 如果当前物品是要分拣的物品，则将该物品向这个方向丢出
     */
    private final Vec3d thisVec;
    /**
     * 如果当前物品不是要分拣的物品，则将该物品向这个方向丢出
     */
    private final Vec3d otherVec;

    public ItemCategorizeAction(EntityPlayerMPFake fakePlayer, ItemStackPredicate predicate, Vec3d thisVec, Vec3d otherVec) {
        super(fakePlayer);
        this.predicate = predicate;
        this.thisVec = thisVec;
        this.otherVec = otherVec;
    }

    @Override
    protected void tick() {
        //获取玩家物品栏对象
        PlayerInventory inventory = this.getFakePlayer().getInventory();
        //遍历玩家物品栏，找到要丢出的物品
        for (int index = 0; index < inventory.size(); index++) {
            //定义变量记录当前槽位的物品堆栈对象
            ItemStack itemStack = inventory.getStack(index);
            if (itemStack.isEmpty()) {
                continue;
            }
            //如果是要分拣的物品，就转向一边，否则转身向另一边
            if (this.predicate.test(itemStack)) {
                this.getFakePlayer().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, thisVec);
            } else {
                //丢弃潜影盒内的物品
                //判断当前物品是不是潜影盒
                if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                    itemStack = pickItemFromShulkerBox(inventory, index);
                } else {
                    //设置当前朝向为丢出非指定物品朝向
                    this.getFakePlayer().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, otherVec);
                }
            }
            //丢弃该物品堆栈
            FakePlayerUtils.dropItem(this.getFakePlayer(), itemStack);
        }
    }

    // 从潜影盒中拿取并分拣物品
    private ItemStack pickItemFromShulkerBox(PlayerInventory inventory, int index) {
        ItemStack itemStack;
        int loopCount = 0;
        while (true) {
            loopCount++;
            if (loopCount > 100) {
                throw new InfiniteLoopException();
            }
            // 一轮循环结束后，再重新将当前物品设置为物品栏中的潜影盒
            itemStack = inventory.getStack(index);
            //判断潜影盒是否为空
            if (InventoryUtils.isEmptyShulkerBox(itemStack)) {
                // 如果为空，将朝向设置为丢出非指定物品的方向，然后结束循环
                // 设置当前朝向为丢出非指定物品朝向
                this.getFakePlayer().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, this.otherVec);
                break;
            } else {
                // 获取潜影盒内第一个非空气物品，获取后，该物品会在潜影盒内删除
                // 设置当前物品为潜影盒内容物的第一个非空物品
                itemStack = InventoryUtils.pickItemFromShulkerBox(itemStack, stack -> !stack.isEmpty());
                if (itemStack.isEmpty()) {
                    itemStack = inventory.getStack(index);
                    // 设置当前朝向为丢出非指定物品朝向，然后丢弃这个潜影盒
                    this.getFakePlayer().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, this.otherVec);
                    break;
                }
                // 根据当前物品设置朝向
                this.getFakePlayer().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, this.predicate.test(itemStack) ? this.thisVec : this.otherVec);
            }
            // 丢弃潜影盒内物品堆栈
            FakePlayerUtils.dropItem(this.getFakePlayer(), itemStack);
        }
        return itemStack;
    }

    @Override
    public List<Text> info() {
        ArrayList<Text> list = new ArrayList<>();
        // 获取要分拣的物品名称
        Text itemName = this.predicate.toText();
        // 获取假玩家的显示名称
        Text fakeName = this.getFakePlayer().getDisplayName();
        // 将假玩家正在分拣物品的消息添加到集合中
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.sorting.predicate", fakeName, itemName));
        // 获取分拣物品要丢出的方向
        Text thisPos = posText(this.thisVec.getX(), this.thisVec.getY(), this.thisVec.getZ());
        // 获取非分拣物品要丢出的方向
        Text otherPos = posText(this.otherVec.getX(), this.otherVec.getY(), this.otherVec.getZ());
        // 将丢要分拣物品的方向的信息添加到集合
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.sorting.this", itemName, thisPos));
        // 将丢其他物品的方向的信息添加到集合
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.sorting.other", otherPos));
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        // 要分拣的物品
        json.addProperty(ITEM, this.predicate.toString());
        // 当前物品要丢弃的位置
        JsonArray thisVecJson = new JsonArray();
        thisVecJson.add(this.thisVec.x);
        thisVecJson.add(this.thisVec.y);
        thisVecJson.add(this.thisVec.z);
        json.add(THIS_VEC, thisVecJson);
        // 其它物品要丢弃的位置
        JsonArray otherVecJson = new JsonArray();
        otherVecJson.add(this.otherVec.x);
        otherVecJson.add(this.otherVec.y);
        otherVecJson.add(this.otherVec.z);
        json.add(OTHER_VEC, otherVecJson);
        return json;
    }

    private Text posText(double x, double y, double z) {
        return TextBuilder.create(String.format("%.2f %.2f %.2f", x, y, z));
    }

    @Override
    public Text getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.sorting");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.CATEGORIZE;
    }
}
