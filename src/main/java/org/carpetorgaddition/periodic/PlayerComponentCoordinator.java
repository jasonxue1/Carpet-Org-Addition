package org.carpetorgaddition.periodic;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import org.carpetorgaddition.periodic.navigator.NavigatorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerComponentCoordinator {
    private final ServerPlayerEntity player;
    @Nullable
    private final FakePlayerActionManager fakePlayerActionManager;
    @Nullable
    private final BlockExcavator blockExcavator;
    private final NavigatorManager navigatorManager;

    public PlayerComponentCoordinator(ServerPlayerEntity player) {
        this.player = player;
        if (player instanceof EntityPlayerMPFake fakePlayer) {
            this.fakePlayerActionManager = new FakePlayerActionManager(fakePlayer);
            this.blockExcavator = new BlockExcavator(fakePlayer);
        } else {
            this.fakePlayerActionManager = null;
            this.blockExcavator = null;
        }
        this.navigatorManager = new NavigatorManager(player);
    }

    public void tick() {
        if (this.fakePlayerActionManager != null) {
            ServerTickManager tickManager = this.player.getWorld().getServer().getTickManager();
            if (tickManager.shouldTick()) {
                this.fakePlayerActionManager.tick();
            }
        }
        if (this.blockExcavator != null) {
            this.blockExcavator.tick();
        }
        this.navigatorManager.tick();
    }

    @Nullable
    public FakePlayerActionManager getFakePlayerActionManager() {
        return this.fakePlayerActionManager;
    }

    @Nullable
    public BlockExcavator getBlockExcavator() {
        return this.blockExcavator;
    }

    public NavigatorManager getNavigatorManager() {
        return this.navigatorManager;
    }

    @NotNull
    public static PlayerComponentCoordinator getManager(ServerPlayerEntity player) {
        return ((PeriodicTaskManagerInterface) player).carpet_Org_Addition$getPlayerPeriodicTaskManager();
    }

    /**
     * <p>
     * <s>玩家通过末地返回传送门时，实际上是创建了一个新对象，然后将原有的数据拷贝到了新对象上，而本类的对象也是玩家的一个成员变量，因此也要进行拷贝。</s>
     * </p>
     * <p>
     * 在{@code 1.21}中通过调试发现这个方法并没有在玩家进入返回传送门时执行，传送逻辑已经被修改了吗？
     * </p>
     */
    public void copyFrom(ServerPlayerEntity oldPlayer) {
        if (this.fakePlayerActionManager != null) {
            this.fakePlayerActionManager.setActionFromOldPlayer((EntityPlayerMPFake) oldPlayer);
        }
        this.navigatorManager.setNavigatorFromOldPlayer(oldPlayer);
    }
}
