package org.carpetorgaddition.periodic;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import org.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import org.carpetorgaddition.util.FetcherUtils;

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
        ServerTickRateManager tickManager = FetcherUtils.getServer(this.getPlayer()).tickRateManager();
        if (tickManager.runsNormally()) {
            this.fakePlayerActionManager.tick();
        }
        this.blockExcavator.tick();
    }

    @Override
    public void copyFrom(ServerPlayer oldPlayer) {
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
