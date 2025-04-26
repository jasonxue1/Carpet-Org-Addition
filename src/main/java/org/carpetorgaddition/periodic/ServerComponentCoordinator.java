package org.carpetorgaddition.periodic;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.periodic.task.ServerTaskManager;
import org.carpetorgaddition.rule.RuleSelfManager;
import org.jetbrains.annotations.NotNull;

public class ServerComponentCoordinator {
    static {
        // 注册服务器保存事件
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> getManager(server).onServerSave());
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
    private final RuleSelfManager ruleSelfManager;

    public ServerComponentCoordinator(MinecraftServer server) {
        this.expressManager = new ExpressManager(server);
        this.server = server;
        this.ruleSelfManager = new RuleSelfManager(server);
    }

    public void tick() {
        this.expressManager.tick();
        ServerTickManager tickManager = this.server.getTickManager();
        this.serverTaskManager.tick(tickManager);
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

    private void onServerSave() {
        this.ruleSelfManager.onServerSave();
    }

    @NotNull
    public static ServerComponentCoordinator getManager(CommandContext<ServerCommandSource> context) {
        return getManager(context.getSource().getServer());
    }

    @NotNull
    public static ServerComponentCoordinator getManager(MinecraftServer server) {
        return ((PeriodicTaskManagerInterface) server).carpet_Org_Addition$getServerPeriodicTaskManager();
    }
}
