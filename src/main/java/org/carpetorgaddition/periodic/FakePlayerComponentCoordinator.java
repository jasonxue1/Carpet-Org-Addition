package org.carpetorgaddition.periodic;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;

public class FakePlayerComponentCoordinator extends PlayerComponentCoordinator {
    private final FakePlayerActionManager fakePlayerActionManager;
    private final BlockExcavator blockExcavator;

    public FakePlayerComponentCoordinator(EntityPlayerMPFake fakePlayer) {
        super(fakePlayer);
        this.fakePlayerActionManager = new FakePlayerActionManager(fakePlayer);
        this.blockExcavator = new BlockExcavator(fakePlayer);
    }

    @Override
    public void tick() {
        super.tick();
        ServerTickManager tickManager = this.getPlayer().getWorld().getServer().getTickManager();
        if (tickManager.shouldTick()) {
            this.fakePlayerActionManager.tick();
        }
        this.blockExcavator.tick();
    }

    @Override
    public void copyFrom(ServerPlayerEntity oldPlayer) {
        super.copyFrom(oldPlayer);
        if (this.fakePlayerActionManager != null) {
            this.fakePlayerActionManager.setActionFromOldPlayer((EntityPlayerMPFake) oldPlayer);
        }
    }

    public FakePlayerActionManager getFakePlayerActionManager() {
        return this.fakePlayerActionManager;
    }

    public BlockExcavator getBlockExcavator() {
        return this.blockExcavator;
    }

    @Override
    protected EntityPlayerMPFake getPlayer() {
        return (EntityPlayerMPFake) super.getPlayer();
    }
}
