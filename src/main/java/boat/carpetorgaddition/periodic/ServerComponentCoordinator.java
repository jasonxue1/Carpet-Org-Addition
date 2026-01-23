package boat.carpetorgaddition.periodic;

import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import boat.carpetorgaddition.periodic.parcel.ParcelManager;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.rule.RuleConfig;
import boat.carpetorgaddition.rule.RuleSelfManager;
import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessManager;
import boat.carpetorgaddition.wheel.page.PageManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ServerComponentCoordinator {
    static {
        // 注册服务器保存事件
        ServerLifecycleEvents.AFTER_SAVE.register((server, _, _) -> getCoordinator(server).onServerSave());
    }

    private final MinecraftServer server;
    /**
     * 快递管理器
     */
    private final ParcelManager parcelManager;
    /**
     * 服务器任务管理器
     */
    private final ServerTaskManager serverTaskManager = new ServerTaskManager();
    /**
     * 翻页管理器
     */
    private final PageManager pageManager;
    private final RuleSelfManager ruleSelfManager;
    private final PlayerSerializationManager playerSerializationManager;
    private final FabricPlayerAccessManager accessManager;
    private final RuleConfig ruleConfig;
    private final DialogProvider dialogProvider;

    public ServerComponentCoordinator(MinecraftServer server) {
        this.server = server;
        this.parcelManager = new ParcelManager(server);
        this.pageManager = new PageManager(server);
        this.ruleSelfManager = new RuleSelfManager(server);
        this.playerSerializationManager = new PlayerSerializationManager(server);
        this.accessManager = new FabricPlayerAccessManager(server);
        this.ruleConfig = new RuleConfig(server);
        this.dialogProvider = new DialogProvider(server);
    }

    public void onServerStarted() {
        this.playerSerializationManager.init();
    }

    public void tick() {
        this.parcelManager.tick();
        ServerTickRateManager tickManager = this.server.tickRateManager();
        this.serverTaskManager.tick(tickManager);
        this.pageManager.tick();
        this.accessManager.tick();
    }

    public ParcelManager getParcelManager() {
        return this.parcelManager;
    }

    public ServerTaskManager getServerTaskManager() {
        return this.serverTaskManager;
    }

    public RuleSelfManager getRuleSelfManager() {
        return this.ruleSelfManager;
    }

    public PageManager getPageManager() {
        return this.pageManager;
    }

    public PlayerSerializationManager getPlayerSerializationManager() {
        return this.playerSerializationManager;
    }

    public FabricPlayerAccessManager getAccessManager() {
        return accessManager;
    }

    public RuleConfig getRuleConfig() {
        return this.ruleConfig;
    }

    public DialogProvider getDialogProvider() {
        return this.dialogProvider;
    }

    private void onServerSave() {
        this.ruleSelfManager.onServerSave();
        this.playerSerializationManager.onServerSave();
    }

    public static ServerComponentCoordinator getCoordinator(MinecraftServer server) {
        return ((PeriodicTaskManagerInterface) server).carpet_Org_Addition$getServerPeriodicTaskManager();
    }
}
