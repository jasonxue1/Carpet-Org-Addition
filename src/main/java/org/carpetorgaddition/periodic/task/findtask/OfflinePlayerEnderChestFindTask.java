package org.carpetorgaddition.periodic.task.findtask;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.util.TextUtils;

import java.io.File;

@SuppressWarnings("JavadocReference")
public class OfflinePlayerEnderChestFindTask extends OfflinePlayerFindTask {
    public OfflinePlayerEnderChestFindTask(
            CommandContext<ServerCommandSource> context,
            UserCache userCache,
            ServerPlayerEntity player,
            File[] files
    ) {
        super(context, userCache, player, files);
    }

    /**
     * 从NBT读取玩家末影箱
     *
     * @see PlayerEntity#readCustomData(ReadView)
     */
    @Override
    protected Inventory getInventory(NbtCompound nbt) {
        EnderChestInventory inventory = new EnderChestInventory();
        ReadView readView = NbtReadView.get(ErrorReporter.EMPTY, this.player.server.getRegistryManager(), nbt);
        inventory.readData(readView.getTypedListView("EnderItems", StackWithSlot.CODEC));
        return inventory;
    }

    @Override
    protected Text getInventoryName() {
        return TextUtils.translate("container.enderchest");
    }
}
