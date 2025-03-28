package org.carpetorgaddition.periodic.task.searchtask;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.ImmutableInventory;

@SuppressWarnings("JavadocReference")
public class OfflinePlayerEnderChestSearchTask extends OfflinePlayerSearchTask {
    public OfflinePlayerEnderChestSearchTask(OfflinePlayerItemSearchContext context) {
        super(context);
    }

    /**
     * 从NBT读取玩家末影箱
     *
     * @see PlayerEntity#readCustomDataFromNbt(NbtCompound)
     */
    @Override
    protected Inventory getInventory(NbtCompound nbt) {
        EnderChestInventory inventory = new EnderChestInventory();
        if (nbt.contains("EnderItems", NbtElement.LIST_TYPE)) {
            inventory.readNbtList(nbt.getList("EnderItems", NbtElement.COMPOUND_TYPE), this.player.getRegistryManager());
            return inventory;
        } else {
            return ImmutableInventory.EMPTY;
        }
    }

    @Override
    protected Text getInventoryName() {
        return TextUtils.translate("container.enderchest");
    }
}
