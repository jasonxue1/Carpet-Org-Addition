package boat.carpetorgaddition.periodic;

import boat.carpetorgaddition.periodic.navigator.NavigatorManager;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class PlayerComponentCoordinator {
    private final ServerPlayer player;
    private final NavigatorManager navigatorManager;
    private final WithButtonPlayerInventory withButtonPlayerInventory;

    public PlayerComponentCoordinator(ServerPlayer player) {
        this.player = player;
        this.navigatorManager = new NavigatorManager(player);
        this.withButtonPlayerInventory = new WithButtonPlayerInventory(player);
    }

    public static PlayerComponentCoordinator of(ServerPlayer serverPlayerEntity) {
        return switch (serverPlayerEntity) {
            case EntityPlayerMPFake fakePlayer -> new FakePlayerComponentCoordinator(fakePlayer);
            case ServerPlayer player -> new PlayerComponentCoordinator(player);
            case null -> throw new NullPointerException("player may not be null");
        };
    }

    public void tick() {
        this.navigatorManager.tick();
        this.withButtonPlayerInventory.tick();
    }

    public NavigatorManager getNavigatorManager() {
        return this.navigatorManager;
    }

    public WithButtonPlayerInventory getWithButtonPlayerInventory() {
        return this.withButtonPlayerInventory;
    }

    @NotNull
    public static PlayerComponentCoordinator getCoordinator(ServerPlayer player) {
        return ((PeriodicTaskManagerInterface) player).carpet_Org_Addition$getPlayerPeriodicTaskManager();
    }

    @NotNull
    public static FakePlayerComponentCoordinator getCoordinator(EntityPlayerMPFake fakePlayer) {
        return (FakePlayerComponentCoordinator) getCoordinator((ServerPlayer) fakePlayer);
    }

    /**
     * <p>
     * <s>玩家通过末地返回传送门时，实际上是创建了一个新对象，然后将原有的数据拷贝到了新对象上，而本类的对象也是玩家的一个成员变量，因此也要进行拷贝。</s>
     * </p>
     * <p>
     * 在{@code 1.21}中通过调试发现这个方法并没有在玩家进入返回传送门时执行，传送逻辑已经被修改了吗？
     * </p>
     */
    public void copyFrom(ServerPlayer oldPlayer) {
        this.navigatorManager.setNavigatorFromOldPlayer(oldPlayer);
    }

    protected ServerPlayer getPlayer() {
        return this.player;
    }
}
