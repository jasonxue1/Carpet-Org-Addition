package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.world.Container;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class OfflinePlayerEnderChestInventory extends OfflinePlayerInventory {
    public OfflinePlayerEnderChestInventory(FabricPlayerAccessor accessor) {
        super(accessor);
    }

    @Override
    public int getContainerSize() {
        return this.accessor.getEnderChest().getContainerSize();
    }

    @Override
    protected Container getInventory() {
        return this.accessor.getEnderChest();
    }
}
