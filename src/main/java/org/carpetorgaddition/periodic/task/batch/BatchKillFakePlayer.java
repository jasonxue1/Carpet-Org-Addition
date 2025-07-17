package org.carpetorgaddition.periodic.task.batch;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.periodic.task.schedule.ReLoginTask;

public class BatchKillFakePlayer extends ServerTask {
    private final PlayerManager playerManager;
    private final String prefix;
    private final int end;
    private int current;

    public BatchKillFakePlayer(MinecraftServer server, String prefix, int start, int end) {
        this.playerManager = server.getPlayerManager();
        this.prefix = prefix;
        this.end = end;
        this.current = start;
    }

    @Override
    protected void tick() {
        long l = System.currentTimeMillis();
        while (this.current <= this.end && System.currentTimeMillis() - l < 30) {
            if (this.playerManager.getPlayer(this.prefix + this.current) instanceof EntityPlayerMPFake fakePlayer) {
                ReLoginTask.logoutPlayer(fakePlayer);
            }
            this.current++;
        }
    }

    @Override
    protected boolean stopped() {
        return this.current > this.end;
    }

    @Override
    public String getLogName() {
        return "玩家批量杀死";
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
