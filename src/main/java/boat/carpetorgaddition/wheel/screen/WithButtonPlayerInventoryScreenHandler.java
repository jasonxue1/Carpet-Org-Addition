package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.mixin.accessor.carpet.EntityPlayerActionPackAccessor;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.wheel.DisabledSlot;
import boat.carpetorgaddition.wheel.inventory.CombinedInventory;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryDecomposer;
import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.Action;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

@NullMarked
public class WithButtonPlayerInventoryScreenHandler extends AbstractContainerMenu {
    /**
     * 玩家正在操作的物品栏
     */
    private final CombinedInventory inventory;
    private final ButtonInventory intervalAttack;
    private final ButtonInventory continuousAttack;
    private final ButtonInventory continuousUse;
    private final ButtonInventory hotbar;
    private final ServerPlayer player;
    private final EntityPlayerActionPack actionPack;
    private static final Item ON = Items.BARRIER;
    private static final Item OFF = Items.STRUCTURE_VOID;

    public WithButtonPlayerInventoryScreenHandler(int containerId, Inventory playerInventory, ServerPlayer player, PlayerInventoryDecomposer decomposer) {
        super(MenuType.GENERIC_9x6, containerId);
        this.player = player;
        this.actionPack = FetcherUtils.getActionPack(this.player);
        ButtonInventory stopAll = new StopButtonInventory(_ -> Map.entry(new ItemStack(ON), new ItemStack(OFF)), (_, pack) -> pack.stopAll());
        this.intervalAttack = new ButtonInventory(
                _ -> Map.entry(new ItemStack(ON), new ItemStack(OFF)),
                Map.entry(
                        (_, pack) -> pack.start(ActionType.ATTACK, Action.interval(12)),
                        (_, pack) -> pack.start(ActionType.ATTACK, Action.once())
                )
        );
        this.continuousAttack = new ButtonInventory(
                _ -> Map.entry(new ItemStack(ON), new ItemStack(OFF)),
                Map.entry(
                        (_, pack) -> pack.start(ActionType.ATTACK, Action.continuous()),
                        (_, pack) -> pack.start(ActionType.ATTACK, Action.once())
                )
        );
        this.continuousUse = new ButtonInventory(
                _ -> Map.entry(new ItemStack(ON), new ItemStack(OFF)),
                Map.entry(
                        (_, pack) -> pack.start(ActionType.USE, Action.continuous()),
                        (_, pack) -> pack.start(ActionType.USE, Action.once())
                )
        );
        this.hotbar = new HotbarButtonInventory(_ -> Map.entry(new ItemStack(ON), new ItemStack(OFF)), (index, pack) -> pack.setSlot(index + 1));
        stopAll.addMutualExclusion(stopAll);
        stopAll.addMutualExclusion(this.intervalAttack);
        stopAll.addMutualExclusion(this.continuousAttack);
        stopAll.addMutualExclusion(this.continuousUse);
        this.intervalAttack.addMutualExclusion(this.continuousAttack);
        this.continuousAttack.addMutualExclusion(this.intervalAttack);
        ArrayList<Container> list = new ArrayList<>();
        list.add(stopAll);
        list.add(decomposer.getArmor());
        list.add(this.intervalAttack);
        list.add(this.continuousAttack);
        list.add(decomposer.getOffHand());
        list.add(this.continuousUse);
        list.add(this.hotbar);
        list.add(decomposer.getStorage());
        list.add(decomposer.getHotbar());
        this.inventory = new CombinedInventory(list);
        this.addInventorySlot();
        this.addPlayerInventorySlot(playerInventory);
        this.addHotSlot(playerInventory);
        this.init();
    }

    private void init() {
        EntityPlayerActionPackAccessor accessor = (EntityPlayerActionPackAccessor) this.actionPack;
        Map<ActionType, Action> actions = accessor.getActions();
        Action attack = actions.get(ActionType.ATTACK);
        if (attack != null) {
            if (attack.interval == 12) {
                this.intervalAttack.setState(0, true);
            } else if (((EntityPlayerActionPackAccessor.ActionAccessor) attack).isContinuous()) {
                this.continuousAttack.setState(0, true);
            }
        }
        Action use = actions.get(ActionType.USE);
        if (use != null) {
            if (((EntityPlayerActionPackAccessor.ActionAccessor) use).isContinuous()) {
                this.continuousUse.setState(0, true);
            }
        }
        int slot = this.player.getInventory().getSelectedSlot();
        this.hotbar.setState(slot, true);
    }

    /**
     * 添加假玩家物品栏槽位（GUI上半部分）
     */
    private void addInventorySlot() {
        int index = 0;
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 9; ++j) {
                // 如果槽位id大于玩家物品栏的大小，添加不可用槽位
                if (this.inventory.getSubInventory(index) instanceof ButtonInventory) {
                    // 添加不可用槽位
                    this.addSlot(new DisabledSlot(this.inventory, index, 8 + j * 18, 18 + i * 18));
                } else {
                    // 添加普通槽位
                    this.addSlot(new Slot(this.inventory, index, 8 + j * 18, 18 + i * 18));
                }
                index++;
            }
        }
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        Container container = this.inventory.getSubInventory(slotIndex);
        if (container instanceof ButtonInventory buttonInventory) {
            buttonInventory.onClickd(buttonInventory == this.hotbar ? slotIndex - 9 : 0, this.actionPack);
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    /**
     * 添加快捷栏槽位
     */
    private void addHotSlot(Inventory inventory) {
        for (int index = 0; index < 9; index++) {
            this.addSlot(new Slot(inventory, index, 8 + index * 18, 161 + 36));
        }
    }

    /**
     * 添加当前玩家物品栏槽位（GUI下版部分）
     */
    private void addPlayerInventorySlot(Inventory inventory) {
        int index = 0;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, index + 9, 8 + j * 18, 103 + i * 18 + 36));
                index++;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static class ButtonInventory extends SimpleContainer {
        protected final Map.Entry<IntObjectBiConsumer<EntityPlayerActionPack>, IntObjectBiConsumer<EntityPlayerActionPack>> consumer;
        private final List<ItemStack> buttonOn;
        private final List<ItemStack> buttonOff;
        protected final List<ButtonInventory> mutualExclusion = new ArrayList<>();

        protected ButtonInventory(int size, IntFunction<Map.Entry<ItemStack, ItemStack>> function, IntObjectBiConsumer<EntityPlayerActionPack> consumer) {
            Map.Entry<IntObjectBiConsumer<EntityPlayerActionPack>, IntObjectBiConsumer<EntityPlayerActionPack>> entry = Map.entry(consumer, consumer);
            this(size, function, entry);
        }

        public ButtonInventory(
                IntFunction<Map.Entry<ItemStack, ItemStack>> function,
                Map.Entry<IntObjectBiConsumer<EntityPlayerActionPack>, IntObjectBiConsumer<EntityPlayerActionPack>> consumer
        ) {
            this(1, function, consumer);
        }

        protected ButtonInventory(
                int size,
                IntFunction<Map.Entry<ItemStack, ItemStack>> function,
                Map.Entry<IntObjectBiConsumer<EntityPlayerActionPack>, IntObjectBiConsumer<EntityPlayerActionPack>> consumer
        ) {
            ArrayList<ItemStack> on = new ArrayList<>(size);
            ArrayList<ItemStack> off = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Map.Entry<ItemStack, ItemStack> entry = function.apply(i);
                on.add(entry.getKey());
                off.add(entry.getValue());
            }
            this.buttonOn = List.copyOf(on);
            this.buttonOff = List.copyOf(off);
            super(off.toArray(ItemStack[]::new));
            this.consumer = consumer;
        }

        public void onClickd(int index, EntityPlayerActionPack actionPack) {
            ItemStack current = this.getItem(index);
            if (current == this.buttonOff.get(index)) {
                this.consumer.getKey().accept(index, actionPack);
                this.setState(index, true);
            } else {
                this.consumer.getValue().accept(index, actionPack);
                this.setState(index, false);
            }
            for (ButtonInventory inventory : this.mutualExclusion) {
                inventory.setState(0, false);
            }
        }

        public void setState(int index, boolean state) {
            this.setItem(index, state ? this.buttonOn.get(index) : this.buttonOff.get(index));
        }

        public void addMutualExclusion(ButtonInventory inventory) {
            this.mutualExclusion.add(inventory);
        }
    }

    public static class StopButtonInventory extends ButtonInventory {
        public StopButtonInventory(IntFunction<Map.Entry<ItemStack, ItemStack>> function, IntObjectBiConsumer<EntityPlayerActionPack> consumer) {
            super(1, function, consumer);
        }

        @Override
        public void onClickd(int index, EntityPlayerActionPack actionPack) {
            this.consumer.getKey().accept(index, actionPack);
            for (ButtonInventory inventory : this.mutualExclusion) {
                inventory.setState(0, false);
            }
        }
    }

    public static class HotbarButtonInventory extends ButtonInventory {
        public HotbarButtonInventory(IntFunction<Map.Entry<ItemStack, ItemStack>> function, IntObjectBiConsumer<EntityPlayerActionPack> consumer) {
            super(9, function, consumer);
        }

        @Override
        public void onClickd(int index, EntityPlayerActionPack actionPack) {
            for (int i = 0; i < this.getContainerSize(); i++) {
                if (i == index) {
                    setState(index, true);
                    this.consumer.getValue().accept(index, actionPack);
                } else {
                    this.setState(i, false);
                }
            }
        }
    }
}
