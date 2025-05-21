package org.carpetorgaddition.periodic.task.search;

import carpet.CarpetSettings;
import carpet.utils.CommandHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.rule.value.OpenPlayerInventory;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.ImmutableInventory;
import org.carpetorgaddition.util.provider.CommandProvider;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    protected Text openInventoryButton(GameProfile gameProfile) {
        if (CommandHelper.canUseCommand(this.source, CarpetSettings.commandPlayer) && OpenPlayerInventory.isEnable(this.source)) {
            String command = CommandProvider.openPlayerEnderChest(gameProfile.getId());
            MutableText clickLogin = TextUtils.translate("carpet.commands.finder.item.offline_player.open.ender_chest");
            return TextUtils.command(TextUtils.createText("[O]"), command, clickLogin, Formatting.GRAY);
        }
        return null;
    }

    @Override
    protected Text getInventoryName() {
        return TextUtils.translate("container.enderchest");
    }
}
