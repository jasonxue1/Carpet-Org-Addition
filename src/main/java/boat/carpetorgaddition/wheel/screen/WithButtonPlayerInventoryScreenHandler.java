package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.wheel.DisabledSlot;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory.ButtonInventory;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory.StopButtonInventory;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public class WithButtonPlayerInventoryScreenHandler extends AbstractContainerMenu implements BackgroundSpriteSyncServer {
    private final WithButtonPlayerInventory inventory;
    private static final Map<Integer, Identifier> BACKGROUND_SPRITE_MAP = Map.of(
            1, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            2, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            3, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            4, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            7, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD
    );

    public WithButtonPlayerInventoryScreenHandler(int containerId, Inventory inventory, ServerPlayer player) {
        super(MenuType.GENERIC_9x6, containerId);
        this.inventory = PlayerComponentCoordinator.getManager(player).getWithButtonPlayerInventory();
        this.addInventorySlot();
        this.addPlayerInventorySlot(inventory);
        this.addHotSlot(inventory);
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
        if (buttonNum == FakePlayerUtils.PICKUP_RIGHT_CLICK && container instanceof StopButtonInventory) {
            this.inventory.sort();
            return;
        }
        if (container instanceof ButtonInventory buttonInventory) {
            buttonInventory.onClickd(buttonInventory == this.inventory.getHotbar() ? slotIndex - 9 : 0, this.inventory.getActionPack());
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

    public static ItemStack quickMoveStack(int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public Map<Integer, Identifier> getBackgroundSprite() {
        return BACKGROUND_SPRITE_MAP;
    }
}
