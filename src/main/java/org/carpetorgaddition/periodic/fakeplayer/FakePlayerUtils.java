package org.carpetorgaddition.periodic.fakeplayer;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.periodic.fakeplayer.action.StopAction;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.inventory.AbstractCustomSizeInventory;
import org.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import org.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FakePlayerUtils {

    /**
     * 槽位外部的索引，相当于点击GUI外面，用来丢弃光标上的物品
     */
    public static final int EMPTY_SPACE_SLOT_INDEX = ScreenHandler.EMPTY_SPACE_SLOT_INDEX;//-999
    /**
     * 模拟左键单击槽位
     */
    public static final int PICKUP_LEFT_CLICK = 0;
    /**
     * 模拟右键单击槽位
     */
    public static final int PICKUP_RIGHT_CLICK = 1;
    /**
     * 模拟按Q键丢弃物品
     */
    public static final int THROW_Q = 0;
    /**
     * 模拟Ctrl+Q丢弃物品
     */
    public static final int THROW_CTRL_Q = 1;
    // 最大循环次数
    public static final int MAX_LOOP_COUNT = 1200;
    /**
     * 模组{@code gugle-carpet-addition}是否已加载
     */
    private static final boolean GCA_LOADED = FabricLoader.getInstance().isModLoaded("gca");
    /**
     * 玩家生存模式物品栏的起始索引
     */
    public static final int PLAYER_INVENTORY_START = 9;
    /**
     * 玩家生存模式物品栏的结束索引
     */
    public static final int PLAYER_INVENTORY_END = 44;

    private FakePlayerUtils() {
    }

    /**
     * 将要丢弃的物品堆栈对象复制一份并丢出，然后将原本的物品堆栈对象删除
     *
     * @param player    当前要丢弃物品的玩家
     * @param itemStack 要丢弃的物品堆栈对象
     * @apiNote 此方法不应用于丢弃GUI中的物品，因为这不会触发{@link ScreenHandler#onSlotClick}的行为
     */
    public static void dropItem(EntityPlayerMPFake player, ItemStack itemStack) {
        player.dropItem(itemStack.copyAndEmpty(), false, false);
    }

    /**
     * 根据条件丢弃物品栏中所有物品
     *
     * @return 是否丢弃了物品
     */
    public static boolean dropInventoryItem(EntityPlayerMPFake fakePlayer, Predicate<ItemStack> predicate) {
        PlayerScreenHandler screenHandler = fakePlayer.playerScreenHandler;
        boolean drop = false;
        for (Slot slot : screenHandler.slots) {
            ItemStack itemStack = slot.getStack();
            if (itemStack.isEmpty()) {
                continue;
            }
            int id = slot.id;
            if (isStorageSlot(id)) {
                if (isEquipmentSlot(id)) {
                    continue;
                }
                if (predicate.test(itemStack)) {
                    throwItem(screenHandler, id, fakePlayer);
                    drop = true;
                }
            }
        }
        return drop;
    }

    /**
     * 整理物品栏
     */
    public static void sorting(EntityPlayerMPFake fakePlayer) {
        PlayerScreenHandler screenHandler = fakePlayer.playerScreenHandler;
        // 记录所有未被锁定的槽位
        ArrayList<Integer> list = new ArrayList<>();
        // 合并相同的物品
        for (int i = PLAYER_INVENTORY_END; i >= PLAYER_INVENTORY_START; i--) {
            if (canClick(fakePlayer, screenHandler, i)) {
                list.add(i);
                ItemStack itemStack = screenHandler.getSlot(i).getStack();
                // 物品不可堆叠或堆叠已满
                if (InventoryUtils.isItemStackFull(itemStack)) {
                    continue;
                }
                // 向前检查并尝试合并可以合并的物品，不需要检查光标上是否有物品
                pickupCursorStack(screenHandler, i, fakePlayer);
                mergeItemStack(fakePlayer, screenHandler, i - 1, PLAYER_INVENTORY_END);
                pickupCursorStack(screenHandler, i, fakePlayer);
            }
        }
        // 整理物品
        sorting(fakePlayer, screenHandler, list.reversed());
    }

    private static void sorting(EntityPlayerMPFake fakePlayer, ScreenHandler screenHandler, List<Integer> list) {
        if (list.isEmpty()) {
            return;
        }
        int start = 0;
        int end = list.size() - 1;
        // 基准物品
        ItemStack pivot = screenHandler.getSlot(list.getFirst()).getStack();
        while (start < end) {
            // 程序是否陷入了死循环
            boolean infiniteLoop = true;
            while (end > start && InventoryUtils.compare(pivot, screenHandler.getSlot(list.get(end)).getStack()) <= 0) {
                end--;
                infiniteLoop = false;
            }
            while (end > start && InventoryUtils.compare(pivot, screenHandler.getSlot(list.get(start)).getStack()) >= 0) {
                start++;
                infiniteLoop = false;
            }
            if (infiniteLoop) {
                throw new InfiniteLoopException("Stuck in an infinite loop while sorting items on the %s".formatted(screenHandler.getClass().getName()));
            }
            swapSlotItem(screenHandler, list.get(start), list.get(end), fakePlayer);
        }
        // 基准物品归位
        swapSlotItem(screenHandler, list.getFirst(), list.get(start), fakePlayer);
        sorting(fakePlayer, screenHandler, list.subList(0, start));
        sorting(fakePlayer, screenHandler, list.subList(start + 1, list.size()));
    }

    @SuppressWarnings("unused")
    private static void sorting(EntityPlayerMPFake fakePlayer, ScreenHandler screenHandler, final int left, final int right) {
        if (left >= right) {
            return;
        }
        int start = left;
        int end = right;
        // 基准物品
        ItemStack pivot = screenHandler.getSlot(left).getStack();
        while (start < end) {
            while (end > start && InventoryUtils.compare(pivot, screenHandler.getSlot(end).getStack()) <= 0) {
                end--;
            }
            while (end > start && InventoryUtils.compare(pivot, screenHandler.getSlot(start).getStack()) >= 0) {
                start++;
            }
            swapSlotItem(screenHandler, start, end, fakePlayer);
        }
        // 基准物品归位
        swapSlotItem(screenHandler, left, start, fakePlayer);
        sorting(fakePlayer, screenHandler, left, start - 1);
        sorting(fakePlayer, screenHandler, start + 1, right);
    }

    /**
     * 合并物品栏栏中相同的物品
     */
    @SuppressWarnings("SameParameterValue")
    private static void mergeItemStack(EntityPlayerMPFake fakePlayer, ScreenHandler screenHandler, int start, int end) {
        for (int i = end; i > start; i--) {
            ItemStack itemStack = screenHandler.getSlot(i).getStack();
            if (itemStack.isEmpty()) {
                continue;
            }
            // 指定槽位是否可以单击
            if (canClick(fakePlayer, screenHandler, i)) {
                ItemStack cursorStack = screenHandler.getCursorStack();
                // 指定物品与光标物品是否可以合并
                if (InventoryUtils.canMergeTo(cursorStack, itemStack)) {
                    // 合并物品
                    FakePlayerUtils.pickupCursorStack(screenHandler, i, fakePlayer);
                    if (screenHandler.getCursorStack().isEmpty()) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * @return 指定槽位是否可以被单击
     */
    private static boolean canClick(EntityPlayerMPFake fakePlayer, ScreenHandler screenHandler, int index) {
        Slot slot = screenHandler.getSlot(index);
        if (slot.canTakePartial(fakePlayer)) {
            ItemStack itemStack = slot.getStack();
            if (itemStack == AbstractCustomSizeInventory.PLACEHOLDER) {
                return false;
            }
            if (isGcaItem(itemStack)) {
                return false;
            }
            if (fakePlayer.currentScreenHandler instanceof QuickShulkerScreenHandler quickShulkerScreenHandler) {
                return itemStack != quickShulkerScreenHandler.getShulkerBox();
            }
            return true;
        }
        return false;
    }

    /**
     * 模拟Ctrl+Q丢弃物品
     *
     * @param screenHandler 假玩家当前打开的GUI
     * @param slotIndex     假玩家当前操作槽位的索引，如果为0，表示按Ctrl+Q丢弃0索引槽位的物品
     * @param player        当前操作的假玩家
     */
    public static void throwItem(ScreenHandler screenHandler, int slotIndex, EntityPlayerMPFake player) {
        screenHandler.onSlotClick(slotIndex, THROW_CTRL_Q, SlotActionType.THROW, player);
    }

    /**
     * 让假玩家停止当前的操作
     *
     * @param source     用来获取玩家管理器对象，然后通过玩家管理器发送消息，source本身不需要发送消息
     * @param fakePlayer 要停止操作的假玩家
     * @param key        停止操作时在聊天栏输出的内容的翻译键
     */
    public static void stopAction(ServerCommandSource source, EntityPlayerMPFake fakePlayer, String key, Object... obj) {
        FetcherUtils.getFakePlayerActionManager(fakePlayer).setAction(new StopAction(fakePlayer));
        MessageUtils.broadcastMessage(
                source.getServer(),
                TextBuilder.combineAll(fakePlayer.getDisplayName(), ": ", TextBuilder.translate(key, obj))
        );
    }

    /**
     * 模拟按住Shift快速移动物品
     *
     * @param screenHandler 当前打开的GUI
     * @param slotIndex     要操作的槽位的索引
     * @param player        当前操作的玩家
     */
    public static ItemStack quickMove(ScreenHandler screenHandler, int slotIndex, EntityPlayerMPFake player) {
        return screenHandler.quickMove(player, slotIndex);
    }

    /**
     * 模拟光标拾取并丢出物品，作用与{@link #throwItem(ScreenHandler, int, EntityPlayerMPFake)}模拟Ctrl+Q丢出物品类似
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param slotIndex     玩家当前操作的索引
     * @param player        当前操作GUI的假玩家
     */
    public static void pickupAndThrow(ScreenHandler screenHandler, int slotIndex, EntityPlayerMPFake player) {
        screenHandler.onSlotClick(slotIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
    }

    /**
     * 通过模拟光标拾取放置物品来快速移动物品，此方法受到规则{@link CarpetOrgAdditionSettings#fakePlayerCraftKeepItem}影响，会先判断当前物品是否不能移动，然后再进行移动物品操作
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param fromIndex     玩家拿取物品槽位的索引索引
     * @param toIndex       玩家放置物品的槽位所以
     * @param player        当前操作GUI的假玩家
     * @return 物品是否可以移动
     */
    public static boolean withKeepPickupAndMoveItemStack(ScreenHandler screenHandler, int fromIndex, int toIndex, EntityPlayerMPFake player) {
        ItemStack itemStack = screenHandler.getSlot(fromIndex).getStack();
        // 如果假玩家合成保留物品启用，并且该物品的数量为1，并且该物品的最大堆叠数大于1
        // 认为这个物品需要保留，不移动物品
        if (CarpetOrgAdditionSettings.fakePlayerCraftKeepItem && itemStack.getCount() == 1 && itemStack.getMaxCount() > 1) {
            return false;
        }
        // 如果鼠标光标上有物品，先把光标上的物品丢弃
        if (!screenHandler.getCursorStack().isEmpty()) {
            screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        }
        screenHandler.onSlotClick(fromIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        // 如果规则假玩家合成保留物品启用，并且该物品的最大堆叠数大于1，就在该槽位上再放回一个物品
        if (CarpetOrgAdditionSettings.fakePlayerCraftKeepItem && screenHandler.getCursorStack().getMaxCount() > 1) {
            screenHandler.onSlotClick(fromIndex, PICKUP_RIGHT_CLICK, SlotActionType.PICKUP, player);
        }
        screenHandler.onSlotClick(toIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        return true;
    }

    public static void pickupAndMoveItemStack(ScreenHandler screenHandler, int fromIndex, int toIndex, EntityPlayerMPFake player) {
        // 如果鼠标光标上有物品，先把光标上的物品丢弃
        if (!screenHandler.getCursorStack().isEmpty()) {
            screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        }
        screenHandler.onSlotClick(fromIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        screenHandler.onSlotClick(toIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
    }

    /**
     * 功能与{@link FakePlayerUtils#pickupAndMoveItemStack(ScreenHandler, int, int, EntityPlayerMPFake)}基本一致，只是本方法使用右键拿取物品，即一次拿取一半的物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param fromIndex     从哪个槽位拿取物品
     * @param toIndex       将物品放在哪个槽位
     * @param player        操作GUI的假玩家
     */
    public static void pickupAndMoveHalfItemStack(ScreenHandler screenHandler, int fromIndex,
                                                  int toIndex, EntityPlayerMPFake player) {
        // 如果鼠标光标上有物品，先把光标上的物品丢弃
        if (!screenHandler.getCursorStack().isEmpty()) {
            screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        }
        // 右击拾取物品
        screenHandler.onSlotClick(fromIndex, PICKUP_RIGHT_CLICK, SlotActionType.PICKUP, player);
        // 放置物品依然是左键单击
        screenHandler.onSlotClick(toIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
    }

    /**
     * 使用循环一个个丢弃槽位中的物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param slotIndex     玩家当前操作槽位的索引
     * @param player        当前操作该GUI的玩家
     */
    @SuppressWarnings("unused")
    public static void loopThrowItem(ScreenHandler screenHandler, int slotIndex, EntityPlayerMPFake player) {
        // 如果光标不为空，那么将无法丢弃槽位上的物品
        InventoryUtils.assertEmptyStack(screenHandler.getCursorStack());
        Slot slot = screenHandler.getSlot(slotIndex);
        Item item = slot.getStack().getItem();
        while (true) {
            ItemStack itemStack = slot.getStack();
            if (itemStack.isEmpty()) {
                return;
            }
            if (itemStack.isOf(item) && slot.canTakeItems(player)) {
                screenHandler.onSlotClick(slotIndex, THROW_Q, SlotActionType.THROW, player);
                continue;
            }
            return;
        }
    }

    /**
     * 比较并丢出槽位物品<br>
     * 如果槽位上的物品与预期物品相同，则丢出槽位上的物品
     *
     * @apiNote 本方法用来丢弃村民交易槽位上的物品
     * @see <a href="https://bugs.mojang.com/browse/MC-157977">MC-157977</a>
     * @see <a href="https://bugs.mojang.com/browse/MC-215441">MC-215441</a>
     */
    public static void compareAndThrow(ScreenHandler screenHandler, int slotIndex, ItemStack itemStack, EntityPlayerMPFake player) {
        InventoryUtils.assertEmptyStack(screenHandler.getCursorStack());
        Slot slot = screenHandler.getSlot(slotIndex);
        while (slot.hasStack() && ItemStack.areItemsAndComponentsEqual(itemStack, slot.getStack()) && slot.canTakeItems(player)) {
            screenHandler.onSlotClick(slotIndex, THROW_Q, SlotActionType.THROW, player);
        }
    }

    /**
     * 收集槽位上的物品
     *
     * @throws IllegalStateException 如果调用时光标上存在物品
     */
    public static void collectItem(ScreenHandler screenHandler, int slotIndex, AutoGrowInventory inventory, EntityPlayerMPFake fakePlayer) {
        InventoryUtils.assertEmptyStack(screenHandler.getCursorStack(), () -> "光标上物品非空");
        // 取出物品的过程中，输出槽位的物品可能会随着输入物品的改变而改变
        // 物品改变后，应停止取出物品，避免合成错误的物品
        Item item = screenHandler.getSlot(slotIndex).getStack().getItem();
        while (true) {
            Slot slot = screenHandler.getSlot(slotIndex);
            if (slot.hasStack() && slot.canTakeItems(fakePlayer) && item == slot.getStack().getItem()) {
                // 拿取槽位上的物品
                screenHandler.onSlotClick(slotIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
                // 将槽位上的物品放入物品栏并清空光标上的物品
                inventory.addStack(screenHandler.getCursorStack());
                screenHandler.setCursorStack(ItemStack.EMPTY);
            } else {
                break;
            }
        }
    }

    /**
     * 丢弃光标上的物品<br>
     * 该物品是玩家鼠标光标上正在被拎起的物品，它会影响玩家对GUI的其它操作，在进行其他操作如向光标上放置物品前应先丢弃光标上的物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param fakePlayer    当前操作该GUI的玩家
     */
    public static void dropCursorStack(ScreenHandler screenHandler, EntityPlayerMPFake fakePlayer) {
        ItemStack itemStack = screenHandler.getCursorStack();
        if (itemStack.isEmpty()) {
            return;
        }
        screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
    }

    /**
     * 交换两个槽位中的物品<br>
     * 如果光标上的物品与两个槽位上的物品均不相同，则光标物品不会影响物品交换
     */
    public static void swapSlotItem(ScreenHandler screenHandler, int index1, int index2, EntityPlayerMPFake fakePlayer) {
        if (index1 == index2) {
            return;
        }
        ItemStack slot1Item = screenHandler.getSlot(index1).getStack();
        ItemStack slot2Item = screenHandler.getSlot(index2).getStack();
        // 两个槽位中的物品可以合并
        if (InventoryUtils.canMerge(slot1Item, slot2Item)) {
            // 不检查光标物品
            int difference = slot1Item.getCount() - slot2Item.getCount();
            if (difference > 0) {
                screenHandler.onSlotClick(index1, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
                for (int i = 0; i < difference; i++) {
                    screenHandler.onSlotClick(index2, PICKUP_RIGHT_CLICK, SlotActionType.PICKUP, fakePlayer);
                }
                screenHandler.onSlotClick(index1, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
            } else if (difference < 0) {
                screenHandler.onSlotClick(index2, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
                for (int i = 0; i < -difference; i++) {
                    screenHandler.onSlotClick(index1, PICKUP_RIGHT_CLICK, SlotActionType.PICKUP, fakePlayer);
                }
                screenHandler.onSlotClick(index2, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
            }
        } else {
            // 拿取槽位1上的物品，单击槽位2，与槽位2物品交互位置，再次单击槽位1，将物品放回
            screenHandler.onSlotClick(index1, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
            screenHandler.onSlotClick(index2, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
            screenHandler.onSlotClick(index1, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
        }
    }

    /**
     * 将光标上的物品放置在槽位中<br>
     * 此方法不会检查对应槽位上有没有物品，因此使用该方法前应保证要放置物品的槽位上没有物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param index         要放置物品的槽位索引
     * @param fakePlayer    当前操作该GUI的玩家
     */
    public static void pickupCursorStack(ScreenHandler screenHandler, int index, EntityPlayerMPFake fakePlayer) {
        screenHandler.onSlotClick(index, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
    }

    /**
     * 指定物品是否为{@code GCA}（假人背包）物品
     */
    public static boolean isGcaItem(ItemStack itemStack) {
        if (GCA_LOADED || CarpetOrgAddition.isDebugDevelopment()) {
            NbtComponent component = itemStack.get(DataComponentTypes.CUSTOM_DATA);
            if (component == null) {
                return false;
            }
            return component.copyNbt().get("GcaClear") != null;
        }
        return false;
    }

    /**
     * 将合适的物品移动到主手
     *
     * @return 是否移动成功
     */
    public static boolean replenishment(EntityPlayerMPFake fakePlayer, Predicate<ItemStack> predicate) {
        return replenishment(fakePlayer, Hand.MAIN_HAND, predicate);
    }

    /**
     * 将合适的物品移动到指定手，玩家不会从盔甲槽拿取物品
     *
     * @return 是否移动成功
     */
    public static boolean replenishment(EntityPlayerMPFake fakePlayer, Hand hand, Predicate<ItemStack> predicate) {
        ItemStack stackInHand = fakePlayer.getStackInHand(hand);
        if (predicate.test(stackInHand)) {
            return true;
        }
        PlayerScreenHandler screenHandler = fakePlayer.playerScreenHandler;
        boolean pickItemFromShulker = CarpetOrgAdditionSettings.fakePlayerCraftPickItemFromShulkerBox;
        ArrayList<Integer> shulkers = new ArrayList<>();
        // 主手槽位
        int headSlot = hand == Hand.MAIN_HAND ? 36 + fakePlayer.getInventory().selectedSlot : 45;
        for (int i = PLAYER_INVENTORY_START; i <= PLAYER_INVENTORY_END; i++) {
            if (i == headSlot) {
                continue;
            }
            if (predicate.test(screenHandler.getSlot(i).getStack())) {
                swapSlotItem(screenHandler, i, headSlot, fakePlayer);
                return true;
            } else if (pickItemFromShulker) {
                ItemStack shulker = screenHandler.getSlot(i).getStack();
                if (shulker.isEmpty()) {
                    continue;
                }
                if (InventoryUtils.isShulkerBoxItem(shulker)) {
                    shulkers.add(i);
                }
            }
        }
        // 从潜影盒获取物品
        if (pickItemFromShulker) {
            for (Integer index : shulkers) {
                ItemStack shulker = screenHandler.getSlot(index).getStack();
                ItemStack picked = InventoryUtils.pickItemFromShulkerBox(shulker, predicate);
                if (picked.isEmpty()) {
                    continue;
                }
                putToEmptySlotOrDrop(fakePlayer, stackInHand);
                fakePlayer.setStackInHand(hand, picked);
                return true;
            }
        }
        return false;
    }

    /**
     * 将物品放入玩家空槽位，如果没有空槽位，则插入到可以接收物品的潜影盒中，如果依然没有空槽位，则丢弃物品。
     */
    public static void putToEmptySlotOrDrop(EntityPlayerMPFake fakePlayer, ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        itemStack = itemStack.copyAndEmpty();
        PlayerInventory inventory = fakePlayer.getInventory();
        inventory.insertStack(itemStack);
        if (itemStack.isEmpty()) {
            return;
        }
        itemStack = InventoryUtils.putItemToInventoryShulkerBox(itemStack, inventory);
        if (itemStack.isEmpty()) {
            return;
        }
        fakePlayer.dropItem(itemStack, false, false);
    }

    public static boolean hasItem(EntityPlayerMPFake fakePlayer, Predicate<ItemStack> predicate) {
        PlayerInventory inventory = fakePlayer.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (predicate.test(inventory.getStack(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 让玩家看向某个方向
     */
    public static void look(EntityPlayerMPFake fakePlayer, Direction direction) {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
        actionPack.look(direction);
    }

    /**
     * 交互主副手物品
     */
    @SuppressWarnings("unused")
    public static void swapHand(EntityPlayerMPFake fakePlayer) {
        ItemStack temp = fakePlayer.getStackInHand(Hand.OFF_HAND);
        fakePlayer.setStackInHand(Hand.OFF_HAND, fakePlayer.getStackInHand(Hand.MAIN_HAND));
        fakePlayer.setStackInHand(Hand.MAIN_HAND, temp);
    }

    /**
     * 获取物品堆栈的可变文本形式：物品名称*堆叠数量
     */
    public static MutableText getWithCountHoverText(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return new TextBuilder("[A]").setHover(Items.AIR.getName()).setColor(Formatting.DARK_GRAY).build();
        }
        // 获取物品堆栈对应的物品ID的首字母，然后转为大写，再放进中括号里
        String capitalizeFirstLetter = getInitial(itemStack);
        MutableText hover = TextBuilder.combineAll(itemStack.getItem().getName(), "*" + itemStack.getCount());
        return new TextBuilder(capitalizeFirstLetter).setHover(hover).build();
    }

    /**
     * 获取物品ID的首字母，然后转为大写，再放进中括号里
     */
    public static String getInitial(ItemStack itemStack) {
        // 将物品名称的字符串切割为命名空间（如果有）和物品id
        String name = Registries.ITEM.getId(itemStack.getItem()).toString();
        String[] split = name.split(":");
        // 获取数组的索引，如果有命名空间，返回1索引，否则返回0索引，即舍弃命名空间
        int index = (split.length == 1) ? 0 : 1;
        // 获取物品id的首字母，然后大写
        return "[" + Character.toUpperCase(split[index].charAt(0)) + "]";
    }

    /**
     * 是否应该因为合成次数过多而停止合成
     *
     * @param craftCount 当前合成次数
     * @return 是否应该停止
     */
    public static boolean shouldStop(int craftCount) {
        if (CarpetOrgAdditionSettings.fakePlayerMaxCraftCount < 0) {
            return false;
        }
        return craftCount >= CarpetOrgAdditionSettings.fakePlayerMaxCraftCount;
    }

    /**
     * 假玩家停止物品合成操作，并广播停止合成的消息
     *
     * @param source       发送消息的消息源
     * @param playerMPFake 需要停止操作的假玩家
     */
    public static void stopCraftAction(ServerCommandSource source, EntityPlayerMPFake playerMPFake) {
        stopAction(source, playerMPFake, "carpet.commands.playerAction.craft");
    }

    /**
     * 指定索引的槽位是否可以用来存储物品
     *
     * @return 如果是合成槽位，返回{@code false}，否则返回{@code true}
     */
    public static boolean isStorageSlot(int index) {
        return MathUtils.isInRange(5, PLAYER_INVENTORY_END, index);
    }

    /**
     * @return 指定索引的槽位是否为盔甲槽位
     */
    public static boolean isEquipmentSlot(int index) {
        return MathUtils.isInRange(5, 8, index);
    }
}