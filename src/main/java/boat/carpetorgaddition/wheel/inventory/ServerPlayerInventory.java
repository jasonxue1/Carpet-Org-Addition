package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

public class ServerPlayerInventory extends AbstractCustomSizeInventory {
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
    public boolean stillValid(@NonNull Player player) {
        // 玩家活着，并且玩家没有被删除
        return !this.player.isDeadOrDying() && !this.player.isRemoved();
    }
}
