package org.carpetorgaddition.wheel.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

public class VillagerInventory extends AbstractCustomSizeInventory {
    private final Villager villager;
    private final SimpleContainer inventory;

    public VillagerInventory(Villager villager) {
        this.villager = villager;
        this.inventory = villager.getInventory();
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    protected Container getInventory() {
        return this.inventory;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        if (villager.isDeadOrDying() || villager.isRemoved()) {
            return false;
        }
        return player.distanceTo(villager) < 8;
    }
}
