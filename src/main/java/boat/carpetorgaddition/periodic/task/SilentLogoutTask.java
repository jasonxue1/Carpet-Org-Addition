package boat.carpetorgaddition.periodic.task;

import boat.carpetorgaddition.periodic.task.schedule.ReLoginTask;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.commands.CommandSourceStack;

public class SilentLogoutTask extends ServerTask {
    private final EntityPlayerMPFake fakePlayer;
    private int time;

    public SilentLogoutTask(CommandSourceStack source, EntityPlayerMPFake fakePlayer, int time) {
        super(source);
        this.fakePlayer = fakePlayer;
        if (time <= 0) {
            throw new IllegalArgumentException();
        }
        this.time = time;
    }

    @Override
    protected void tick() {
        this.time--;
        if (this.time <= 0) {
            ReLoginTask.logoutPlayer(this.fakePlayer);
        }
    }

    @Override
    protected boolean stopped() {
        return this.fakePlayer.isRemoved() || this.time <= 0;
    }

    @Override
    public String getLogName() {
        return "静默延迟退出";
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
