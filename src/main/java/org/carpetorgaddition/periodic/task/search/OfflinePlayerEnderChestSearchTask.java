package org.carpetorgaddition.periodic.task.search;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import org.carpetorgaddition.util.TextUtils;

@SuppressWarnings("JavadocReference")
public class OfflinePlayerEnderChestSearchTask extends OfflinePlayerSearchTask {
    public OfflinePlayerEnderChestSearchTask(OfflinePlayerItemSearchContext context) {
        super(context);
    }

    /**
     * 从NBT读取玩家末影箱
     *
     * @see PlayerEntity#readCustomData(ReadView)
     */
    @Override
    protected Inventory getInventory(NbtCompound nbt) {
        EnderChestInventory inventory = new EnderChestInventory();
        ReadView readView = NbtReadView.create(ErrorReporter.EMPTY, this.player.getWorld().getRegistryManager(), nbt);
        inventory.readData(readView.getTypedListView("EnderItems", StackWithSlot.CODEC));
        return inventory;
    }

    @Override
    protected Text getInventoryName() {
        return TextUtils.translate("container.enderchest");
    }
}
