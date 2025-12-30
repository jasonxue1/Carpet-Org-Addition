package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.fakeplayer.action.CraftingTableCraftAction;
import boat.carpetorgaddition.periodic.fakeplayer.action.StopAction;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FakePlayerUtils {
    /**
     * 槽位外部的索引，相当于点击GUI外面，用来丢弃光标上的物品
     */
    public static final int EMPTY_SPACE_SLOT_INDEX = AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;//-999
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
    /**
     * 最大循环次数
     */
    public static final int MAX_LOOP_COUNT = 1200;

    private FakePlayerUtils() {
    }

    /**
     * 将要丢弃的物品堆栈对象复制一份并丢出，然后将原本的物品堆栈对象删除
     *
     * @param player    当前要丢弃物品的玩家
     * @param itemStack 要丢弃的物品堆栈对象
     * @apiNote 此方法不应用于丢弃GUI中的物品，因为这不会触发{@link AbstractContainerMenu#clicked}的行为
     */
    public static void dropItem(EntityPlayerMPFake player, ItemStack itemStack) {
        player.drop(itemStack.copyAndClear(), false, false);
    }

    /**
     * 模拟Ctrl+Q丢弃物品
     *
     * @param screenHandler 假玩家当前打开的GUI
     * @param slotIndex     假玩家当前操作槽位的索引，如果为0，表示按Ctrl+Q丢弃0索引槽位的物品
     * @param player        当前操作的假玩家
     */
    public static void throwItem(AbstractContainerMenu screenHandler, int slotIndex, EntityPlayerMPFake player) {
        screenHandler.clicked(slotIndex, THROW_CTRL_Q, ContainerInput.THROW, player);
    }

    /**
     * 让假玩家停止当前的操作
     *
     * @param source     用来获取玩家管理器对象，然后通过玩家管理器发送消息，source本身不需要发送消息
     * @param fakePlayer 要停止操作的假玩家
     * @param message    停止操作时在聊天栏输出的内容
     */
    public static void stopAction(CommandSourceStack source, EntityPlayerMPFake fakePlayer, Component message) {
        FetcherUtils.getFakePlayerActionManager(fakePlayer).setAction(new StopAction(fakePlayer));
        MessageUtils.broadcastMessage(source.getServer(), TextBuilder.combineAll(fakePlayer.getDisplayName(), ": ", message));
    }

    /**
     * 模拟按住Shift快速移动物品
     *
     * @param screenHandler 当前打开的GUI
     * @param slotIndex     要操作的槽位的索引
     * @param player        当前操作的玩家
     */
    public static ItemStack quickMove(AbstractContainerMenu screenHandler, int slotIndex, EntityPlayerMPFake player) {
        return screenHandler.quickMoveStack(player, slotIndex);
    }

    /**
     * 模拟光标拾取并丢出物品，作用与{@link #throwItem(AbstractContainerMenu, int, EntityPlayerMPFake)}模拟Ctrl+Q丢出物品类似
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param slotIndex     玩家当前操作的索引
     * @param player        当前操作GUI的假玩家
     */
    public static void pickupAndThrow(AbstractContainerMenu screenHandler, int slotIndex, EntityPlayerMPFake player) {
        screenHandler.clicked(slotIndex, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
        screenHandler.clicked(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
    }

    /**
     * 通过模拟光标拾取放置物品来快速移动物品，此方法受到规则{@link CarpetOrgAdditionSettings#fakePlayerActionKeepItem}影响，会先判断当前物品是否不能移动，然后再进行移动物品操作
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param fromIndex     玩家拿取物品槽位的索引索引
     * @param toIndex       玩家放置物品的槽位所以
     * @param player        当前操作GUI的假玩家
     * @return 物品是否可以移动
     */
    public static boolean withKeepPickupAndMoveItemStack(AbstractContainerMenu screenHandler, int fromIndex, int toIndex, EntityPlayerMPFake player) {
        ItemStack itemStack = screenHandler.getSlot(fromIndex).getItem();
        // 如果假玩家合成保留物品启用，并且该物品的数量为1，并且该物品的最大堆叠数大于1
        // 认为这个物品需要保留，不移动物品
        if (CarpetOrgAdditionSettings.fakePlayerActionKeepItem.get() && itemStack.getCount() == 1 && itemStack.getMaxStackSize() > 1) {
            return false;
        }
        // 如果鼠标光标上有物品，先把光标上的物品丢弃
        if (!screenHandler.getCarried().isEmpty()) {
            screenHandler.clicked(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
        }
        screenHandler.clicked(fromIndex, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
        // 如果规则假玩家合成保留物品启用，并且该物品的最大堆叠数大于1，就在该槽位上再放回一个物品
        if (CarpetOrgAdditionSettings.fakePlayerActionKeepItem.get() && screenHandler.getCarried().getMaxStackSize() > 1) {
            screenHandler.clicked(fromIndex, PICKUP_RIGHT_CLICK, ContainerInput.PICKUP, player);
        }
        screenHandler.clicked(toIndex, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
        return true;
    }

    public static void pickupAndMoveItemStack(AbstractContainerMenu screenHandler, int fromIndex, int toIndex, EntityPlayerMPFake player) {
        // 如果鼠标光标上有物品，先把光标上的物品丢弃
        if (!screenHandler.getCarried().isEmpty()) {
            screenHandler.clicked(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
        }
        screenHandler.clicked(fromIndex, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
        screenHandler.clicked(toIndex, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
    }

    /**
     * 功能与{@link FakePlayerUtils#pickupAndMoveItemStack(AbstractContainerMenu, int, int, EntityPlayerMPFake)}基本一致，只是本方法使用右键拿取物品，即一次拿取一半的物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param fromIndex     从哪个槽位拿取物品
     * @param toIndex       将物品放在哪个槽位
     * @param player        操作GUI的假玩家
     */
    public static void pickupAndMoveHalfItemStack(AbstractContainerMenu screenHandler, int fromIndex, int toIndex, EntityPlayerMPFake player) {
        // 如果鼠标光标上有物品，先把光标上的物品丢弃
        if (!screenHandler.getCarried().isEmpty()) {
            screenHandler.clicked(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
        }
        // 右击拾取物品
        screenHandler.clicked(fromIndex, PICKUP_RIGHT_CLICK, ContainerInput.PICKUP, player);
        // 放置物品依然是左键单击
        screenHandler.clicked(toIndex, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, player);
    }

    /**
     * 使用循环一个个丢弃槽位中的物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param slotIndex     玩家当前操作槽位的索引
     * @param player        当前操作该GUI的玩家
     */
    @SuppressWarnings("unused")
    public static void loopThrowItem(AbstractContainerMenu screenHandler, int slotIndex, EntityPlayerMPFake player) {
        // 如果光标不为空，那么将无法丢弃槽位上的物品
        InventoryUtils.assertEmptyStack(screenHandler.getCarried());
        Slot slot = screenHandler.getSlot(slotIndex);
        Item item = slot.getItem().getItem();
        while (true) {
            ItemStack itemStack = slot.getItem();
            if (itemStack.isEmpty()) {
                return;
            }
            if (itemStack.is(item) && slot.mayPickup(player)) {
                screenHandler.clicked(slotIndex, THROW_Q, ContainerInput.THROW, player);
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
    public static void compareAndThrow(AbstractContainerMenu screenHandler, int slotIndex, ItemStack itemStack, EntityPlayerMPFake player) {
        InventoryUtils.assertEmptyStack(screenHandler.getCarried());
        Slot slot = screenHandler.getSlot(slotIndex);
        while (slot.hasItem() && ItemStack.isSameItemSameComponents(itemStack, slot.getItem()) && slot.mayPickup(player)) {
            screenHandler.clicked(slotIndex, THROW_Q, ContainerInput.THROW, player);
        }
    }

    /**
     * 收集槽位上的物品
     *
     * @throws IllegalStateException 如果调用时光标上存在物品
     */
    public static void collectItem(AbstractContainerMenu screenHandler, int slotIndex, AutoGrowInventory inventory, EntityPlayerMPFake fakePlayer) {
        InventoryUtils.assertEmptyStack(screenHandler.getCarried(), () -> "光标上物品非空");
        // 取出物品的过程中，输出槽位的物品可能会随着输入物品的改变而改变
        // 物品改变后，应停止取出物品，避免合成错误的物品
        Item item = screenHandler.getSlot(slotIndex).getItem().getItem();
        while (true) {
            Slot slot = screenHandler.getSlot(slotIndex);
            if (slot.hasItem() && slot.mayPickup(fakePlayer) && item == slot.getItem().getItem()) {
                // 拿取槽位上的物品
                screenHandler.clicked(slotIndex, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, fakePlayer);
                // 将槽位上的物品放入物品栏并清空光标上的物品
                inventory.addStack(screenHandler.getCarried());
                screenHandler.setCarried(ItemStack.EMPTY);
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
    public static void dropCursorStack(AbstractContainerMenu screenHandler, EntityPlayerMPFake fakePlayer) {
        ItemStack itemStack = screenHandler.getCarried();
        if (itemStack.isEmpty()) {
            return;
        }
        screenHandler.clicked(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, fakePlayer);
    }

    /**
     * 将光标上的物品放置在槽位中<br>
     * 此方法不会检查对应槽位上有没有物品，因此使用该方法前应保证要放置物品的槽位上没有物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param index         要放置物品的槽位索引
     * @param fakePlayer    当前操作该GUI的玩家
     */
    public static void pickupCursorStack(AbstractContainerMenu screenHandler, int index, EntityPlayerMPFake fakePlayer) {
        screenHandler.clicked(index, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, fakePlayer);
    }

    /**
     * 让玩家看向某个方向
     */
    public static void look(EntityPlayerMPFake fakePlayer, Direction direction) {
        EntityPlayerActionPack actionPack = getActionPack(fakePlayer);
        actionPack.look(direction);
    }

    public static void click(EntityPlayerMPFake fakePlayer, InteractionHand hand) {
        EntityPlayerActionPack actionPack = getActionPack(fakePlayer);
        EntityPlayerActionPack.ActionType type = switch (hand) {
            case MAIN_HAND -> EntityPlayerActionPack.ActionType.ATTACK;
            case OFF_HAND -> EntityPlayerActionPack.ActionType.USE;
        };
        actionPack.start(type, EntityPlayerActionPack.Action.once());
    }

    public static EntityPlayerActionPack getActionPack(ServerPlayer player) {
        return ((ServerPlayerInterface) player).getActionPack();
    }

    /**
     * 交互主副手物品
     */
    @SuppressWarnings("unused")
    public static void swapHand(EntityPlayerMPFake fakePlayer) {
        ItemStack temp = fakePlayer.getItemInHand(InteractionHand.OFF_HAND);
        fakePlayer.setItemInHand(InteractionHand.OFF_HAND, fakePlayer.getItemInHand(InteractionHand.MAIN_HAND));
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, temp);
    }

    /**
     * 获取物品堆栈的可变文本形式：物品名称*堆叠数量
     */
    public static Component getWithCountHoverText(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return new TextBuilder("[A]").setHover(Items.AIR.getName()).setColor(ChatFormatting.DARK_GRAY).build();
        }
        // 获取物品堆栈对应的物品ID的首字母，然后转为大写，再放进中括号里
        String capitalizeFirstLetter = getInitial(itemStack);
        Component hover = TextBuilder.combineAll(itemStack.getItem().getName(), "*" + itemStack.getCount());
        return new TextBuilder(capitalizeFirstLetter).setHover(hover).build();
    }

    /**
     * 获取物品ID的首字母，然后转为大写，再放进中括号里
     */
    public static String getInitial(ItemStack itemStack) {
        // 将物品名称的字符串切割为命名空间（如果有）和物品id
        String name = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
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
        if (CarpetOrgAdditionSettings.fakePlayerMaxItemOperationCount.get() < 0) {
            return false;
        }
        return craftCount >= CarpetOrgAdditionSettings.fakePlayerMaxItemOperationCount.get();
    }

    /**
     * 假玩家停止物品合成操作，并广播停止合成的消息
     *
     * @param source       发送消息的消息源
     * @param playerMPFake 需要停止操作的假玩家
     */
    public static void stopCraftAction(CommandSourceStack source, EntityPlayerMPFake playerMPFake) {
        stopAction(source, playerMPFake, CraftingTableCraftAction.KEY.then("error").translate());
    }
}