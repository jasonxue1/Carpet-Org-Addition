package boat.carpetorgaddition.periodic;

import boat.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;

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
        ServerTickRateManager tickManager = ServerUtils.getServer(this.getPlayer()).tickRateManager();
        if (tickManager.runsNormally()) {
            this.fakePlayerActionManager.tick();
        }
        this.blockExcavator.tick();
    }

    @Override
    public void copyFrom(ServerPlayer oldPlayer) {
        super.copyFrom(oldPlayer);
        // TODO 可以为null吗？
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
