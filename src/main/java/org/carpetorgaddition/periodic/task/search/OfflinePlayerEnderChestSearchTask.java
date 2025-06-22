package org.carpetorgaddition.periodic.task.search;

import carpet.CarpetSettings;
import carpet.utils.CommandHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.rule.value.OpenPlayerInventory;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.provider.CommandProvider;

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
    protected Text openInventoryButton(GameProfile gameProfile) {
        if (CommandHelper.canUseCommand(this.source, CarpetSettings.commandPlayer) && OpenPlayerInventory.isEnable(this.source)) {
            String command = CommandProvider.openPlayerEnderChest(gameProfile.getId());
            MutableText clickLogin = TextBuilder.translate("carpet.commands.finder.item.offline_player.open.ender_chest");
            TextBuilder builder = new TextBuilder("[O]");
            builder.setColor(Formatting.GRAY);
            builder.setHover(clickLogin);
            builder.setCommand(command);
            return builder.build();
        }
        return null;
    }

    @Override
    protected Text getInventoryName() {
        return TextBuilder.translate("container.enderchest");
    }
}
