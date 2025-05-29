package org.carpetorgaddition.util.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.util.inventory.ServerPlayerInventory;

public class PlayerInventoryScreenHandler extends AbstractPlayerInventoryScreenHandler<ServerPlayerInventory> {
    private static final Map<Integer, Identifier> BACKGROUND_SPRITE_MAP;

    static {
        BACKGROUND_SPRITE_MAP = Map.of(
                36, PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE,
                37, PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
                38, PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE,
                39, PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE,
                40, PlayerScreenHandler.EMPTY_OFF_HAND_SLOT_TEXTURE
        );
    }

    private static final int SIZE = 41;
    private final ServerPlayerEntity player;

    public PlayerInventoryScreenHandler(int syncId, PlayerInventory inventory, ServerPlayerEntity player) {
        super(syncId, inventory, new ServerPlayerInventory(player));
        this.player = player;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // 玩家活着，并且玩家没有被删除
        return !this.player.isDead() && !this.player.isRemoved();
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.dropExcess(player);
    }
}
