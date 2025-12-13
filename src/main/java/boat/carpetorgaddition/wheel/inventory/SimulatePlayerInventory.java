package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Optional;

/**
 * 模拟一个玩家物品栏，用来接收读取自NBT的物品
 */
public class SimulatePlayerInventory implements Container {
    private final NonNullList<ItemStack> main = NonNullList.withSize(36, ItemStack.EMPTY);
    private final EntityEquipment equipment = new EntityEquipment();

    private SimulatePlayerInventory() {
    }

    /**
     * @see net.minecraft.world.entity.player.Inventory#load(ValueInput.TypedInputList)
     */
    public static SimulatePlayerInventory of(CompoundTag nbt, MinecraftServer server) {
        ValueInput readView = TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), nbt);
        SimulatePlayerInventory inventory = new SimulatePlayerInventory();
        for (ItemStackWithSlot stackWithSlot : readView.listOrEmpty("Inventory", ItemStackWithSlot.CODEC)) {
            if (stackWithSlot.isValidInContainer(inventory.main.size())) {
                inventory.setItem(stackWithSlot.slot(), stackWithSlot.stack());
            }
        }
        inventory.equipment.setAll(readView.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new));
        return inventory;
    }

    @Override
    public int getContainerSize() {
        return this.main.size() + Inventory.EQUIPMENT_SLOT_MAPPING.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.main) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        for (EquipmentSlot equipmentSlot : Inventory.EQUIPMENT_SLOT_MAPPING.values()) {
            if (this.equipment.get(equipmentSlot).isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < this.main.size()) {
            return this.main.get(slot);
        }
        Optional<EquipmentSlot> optional = getEquipmentSlot(slot);
        return optional.map(this.equipment::get).orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < this.main.size()) {
            return ContainerHelper.removeItem(this.main, slot, amount);
        } else {
            Optional<EquipmentSlot> optional = getEquipmentSlot(slot);
            if (optional.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack itemStack = this.equipment.get(optional.get());
            if (!itemStack.isEmpty()) {
                return itemStack.split(amount);
            }
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < this.main.size()) {
            ItemStack itemStack = this.main.get(slot);
            this.main.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
        Optional<EquipmentSlot> optional = getEquipmentSlot(slot);
        return optional.map(equipmentSlot -> this.equipment.set(equipmentSlot, ItemStack.EMPTY)).orElse(ItemStack.EMPTY);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < this.main.size()) {
            this.main.set(slot, stack);
        }
        Optional<EquipmentSlot> optional = getEquipmentSlot(slot);
        if (optional.isEmpty()) {
            return;
        }
        this.equipment.set(optional.get(), stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.main.clear();
        this.equipment.clear();
    }

    @SuppressWarnings("OptionalOfNullableMisuse")
    private Optional<EquipmentSlot> getEquipmentSlot(int slot) {
        return Optional.ofNullable(Inventory.EQUIPMENT_SLOT_MAPPING.get(slot));
    }
}
