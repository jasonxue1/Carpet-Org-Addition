package boat.carpetorgaddition.periodic.task.batch;

import boat.carpetorgaddition.periodic.task.ServerTask;
import boat.carpetorgaddition.periodic.task.schedule.ReLoginTask;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

public class BatchKillFakePlayer extends ServerTask {
    private final PlayerList playerManager;
    private final String prefix;
    private final int end;
    private int current;

    public BatchKillFakePlayer(MinecraftServer server, CommandSourceStack source, String prefix, int start, int end) {
        super(source);
        this.playerManager = server.getPlayerList();
        this.prefix = prefix;
        this.end = end;
        this.current = start;
    }

    @Override
    protected void tick() {
        long l = System.currentTimeMillis();
        while (this.current <= this.end && System.currentTimeMillis() - l < 30) {
            if (this.playerManager.getPlayerByName(this.prefix + this.current) instanceof EntityPlayerMPFake fakePlayer) {
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
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
