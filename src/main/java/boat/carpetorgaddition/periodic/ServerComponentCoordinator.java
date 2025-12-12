package boat.carpetorgaddition.periodic;

import boat.carpetorgaddition.periodic.express.ExpressManager;
import boat.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.rule.RuleConfig;
import boat.carpetorgaddition.rule.RuleSelfManager;
import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessManager;
import boat.carpetorgaddition.wheel.page.PageManager;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import org.jetbrains.annotations.NotNull;

public class ServerComponentCoordinator {
    static {
        // 注册服务器保存事件
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> getCoordinator(server).onServerSave());
    }

    private final MinecraftServer server;
    /**
     * 快递管理器
     */
    private final ExpressManager expressManager;
    /**
     * 服务器任务管理器
     */
    private final ServerTaskManager serverTaskManager = new ServerTaskManager();
    /**
     * 翻页管理器
     */
    private final PageManager pageManager = new PageManager();
    private final RuleSelfManager ruleSelfManager;
    private final PlayerSerializationManager playerSerializationManager;
    private final FabricPlayerAccessManager accessManager;
    private final RuleConfig ruleConfig;

    public ServerComponentCoordinator(MinecraftServer server) {
        this.expressManager = new ExpressManager(server);
        this.server = server;
        this.ruleSelfManager = new RuleSelfManager(server);
        this.playerSerializationManager = new PlayerSerializationManager(server);
        this.accessManager = new FabricPlayerAccessManager(server);
        this.ruleConfig = new RuleConfig(server);
    }

    public void tick() {
        this.expressManager.tick();
        ServerTickRateManager tickManager = this.server.tickRateManager();
        this.serverTaskManager.tick(tickManager);
        this.pageManager.tick();
        this.accessManager.tick();
    }

    public ExpressManager getExpressManager() {
        return this.expressManager;
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

    private void onServerSave() {
        this.ruleSelfManager.onServerSave();
        this.playerSerializationManager.onServerSave();
    }

    @NotNull
    public static ServerComponentCoordinator getCoordinator(CommandContext<CommandSourceStack> context) {
        return getCoordinator(context.getSource().getServer());
    }

    @NotNull
    public static ServerComponentCoordinator getCoordinator(MinecraftServer server) {
        return ((PeriodicTaskManagerInterface) server).carpet_Org_Addition$getServerPeriodicTaskManager();
    }
}
