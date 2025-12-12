package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.wheel.inventory.OfflinePlayerInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

public class OfflinePlayerInventoryScreenHandler extends AbstractPlayerInventoryScreenHandler<OfflinePlayerInventory> {
    public OfflinePlayerInventoryScreenHandler(int syncId, Inventory playerInventory, OfflinePlayerInventory inventory) {
        super(syncId, playerInventory, inventory);
        this.inventory.startOpen(playerInventory.player);
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }
}
