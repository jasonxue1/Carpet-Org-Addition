package org.carpetorgaddition.util.inventory;

import net.minecraft.entity.EntityEquipment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.collection.DefaultedList;

/**
 * 模拟一个玩家物品栏，用来接收读取自NBT的物品
 */
public class SimulatePlayerInventory implements Inventory {
    private final DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
    private final EntityEquipment equipment = new EntityEquipment();

    private SimulatePlayerInventory() {
    }

    /**
     * @see net.minecraft.entity.player.PlayerInventory#readNbt(NbtList)
     */
    public static SimulatePlayerInventory of(NbtCompound nbt, MinecraftServer server) {
        ReadView readView = NbtReadView.create(ErrorReporter.EMPTY, server.getRegistryManager(), nbt);
        SimulatePlayerInventory inventory = new SimulatePlayerInventory();
        for (StackWithSlot stackWithSlot : readView.getTypedListView("Inventory", StackWithSlot.CODEC)) {
            if (stackWithSlot.isValidSlot(inventory.main.size())) {
                inventory.setStack(stackWithSlot.slot(), stackWithSlot.stack());
            }
        }
        inventory.equipment.copyFrom(readView.read("equipment", EntityEquipment.CODEC).orElseGet(EntityEquipment::new));
        return inventory;
    }

    @Override
    public int size() {
        return this.main.size() + PlayerInventory.EQUIPMENT_SLOTS.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.main) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        for (EquipmentSlot equipmentSlot : PlayerInventory.EQUIPMENT_SLOTS.values()) {
            if (this.equipment.get(equipmentSlot).isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < this.main.size()) {
            return this.main.get(slot);
        }
        EquipmentSlot equipmentSlot = PlayerInventory.EQUIPMENT_SLOTS.get(slot);
        return equipmentSlot == null ? ItemStack.EMPTY : this.equipment.get(equipmentSlot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < this.main.size()) {
            return Inventories.splitStack(this.main, slot, amount);
        } else {
            EquipmentSlot equipmentSlot = PlayerInventory.EQUIPMENT_SLOTS.get(slot);
            if (equipmentSlot == null) {
                return ItemStack.EMPTY;
            }
            ItemStack itemStack = this.equipment.get(equipmentSlot);
            if (!itemStack.isEmpty()) {
                return itemStack.split(amount);
            }
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot < this.main.size()) {
            ItemStack itemStack = this.main.get(slot);
            this.main.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
        EquipmentSlot equipmentSlot = PlayerInventory.EQUIPMENT_SLOTS.get(slot);
        return equipmentSlot == null ? ItemStack.EMPTY : this.equipment.put(equipmentSlot, ItemStack.EMPTY);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < this.main.size()) {
            this.main.set(slot, stack);
        }
        EquipmentSlot equipmentSlot = PlayerInventory.EQUIPMENT_SLOTS.get(slot);
        if (equipmentSlot != null) {
            this.equipment.put(equipmentSlot, stack);
        }
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        this.main.clear();
        this.equipment.clear();
    }
}
