package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ServerPlayerInventory extends AbstractCustomSizeInventory implements PlayerInventoryDecomposer {
    private final ServerPlayer player;
    private final Inventory inventory;

    public ServerPlayerInventory(ServerPlayer player) {
        this.player = player;
        this.inventory = player.getInventory();
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @Override
    protected Container getInventory() {
        return this.inventory;
    }

    @Override
    public boolean stillValid(Player player) {
        // 玩家活着，并且玩家没有被删除
        return !this.player.isDeadOrDying() && !this.player.isRemoved();
    }

    @Override
    public Container getStorage() {
        return new SubInventory(this.inventory, 9, 36);
    }

    @Override
    public Container getHotbar() {
        return new SubInventory(this.inventory, 0, 9);
    }

    @Override
    public Container getArmor() {
        return new ReverseInventroy(new SubInventory(this.inventory, 36, 40));
    }

    @Override
    public Container getOffHand() {
        return new SubInventory(this.inventory, 40, 41);
    }
}
