package org.carpetorgaddition.periodic.task;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerStartupAction;

public class FakePlayerStartupActionTask extends ServerTask {
    private final EntityPlayerMPFake fakePlayer;
    private final FakePlayerStartupAction action;
    private int delay;

    public FakePlayerStartupActionTask(ServerCommandSource source, EntityPlayerMPFake fakePlayer, FakePlayerStartupAction action, int delay) {
        super(source);
        this.fakePlayer = fakePlayer;
        this.action = action;
        this.delay = delay;
    }

    @Override
    protected void tick() {
        this.delay--;
        if (this.delay == 0) {
            this.action.accept(this.fakePlayer);
        }
    }

    @Override
    protected boolean stopped() {
        return this.delay < 0;
    }

    @Override
    public String getLogName() {
        return "Fake Player Startup Action";
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            FakePlayerStartupActionTask that = (FakePlayerStartupActionTask) obj;
            return this.fakePlayer.equals(that.fakePlayer) && this.action == that.action;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.action.hashCode();
    }
}
