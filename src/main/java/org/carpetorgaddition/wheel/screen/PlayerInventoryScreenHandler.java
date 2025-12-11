package org.carpetorgaddition.wheel.screen;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import org.carpetorgaddition.wheel.inventory.ServerPlayerInventory;
import org.jspecify.annotations.NonNull;

public class PlayerInventoryScreenHandler extends AbstractPlayerInventoryScreenHandler<ServerPlayerInventory> {
    private final ServerPlayer player;

    public PlayerInventoryScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
        super(syncId, inventory, new ServerPlayerInventory(player));
        this.player = player;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        // 玩家活着，并且玩家没有被删除
        return !this.player.isDeadOrDying() && !this.player.isRemoved();
    }

    @Override
    public void clicked(int slotIndex, int button, @NonNull ClickType actionType, @NonNull Player player) {
        if (CarpetOrgAddition.isDebugDevelopment() && MathUtils.isInRange(this.from(), this.to(), slotIndex) && this.player instanceof EntityPlayerMPFake fakePlayer) {
            PlayerStorageInventory inventory = new PlayerStorageInventory(fakePlayer);
            inventory.sort();
        }
        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }
}
