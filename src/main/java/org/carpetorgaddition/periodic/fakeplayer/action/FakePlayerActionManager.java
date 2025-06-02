package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.jetbrains.annotations.NotNull;

public class FakePlayerActionManager {
    private final EntityPlayerMPFake fakePlayer;
    @NotNull
    private AbstractPlayerAction action;

    public FakePlayerActionManager(EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
        this.action = new StopAction(this.fakePlayer);
    }

    public void tick() {
        try {
            // 根据假玩家动作类型执行动作
            this.action.execute();
        } catch (RuntimeException e) {
            // 将错误信息写入日志
            CarpetOrgAddition.LOGGER.error(
                    "{}在执行“{}”时遇到意外错误:",
                    this.fakePlayer.getName().getString(),
                    this.getAction().getClass().getSimpleName(),
                    e
            );
            MessageUtils.broadcastErrorMessage(
                    this.fakePlayer.getWorld().getServer(),
                    e,
                    "carpet.commands.playerAction.exception.runtime",
                    this.fakePlayer.getDisplayName(),
                    this.getAction().getDisplayName()
            );
            // 让假玩家停止当前操作
            this.stop();
        }
    }

    // 从另一个玩家浅拷贝此动作管理器
    public void setActionFromOldPlayer(EntityPlayerMPFake oldPlayer) {
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(oldPlayer);
        this.setAction(actionManager.getAction());
        this.action.setFakePlayer(this.fakePlayer);
    }

    @NotNull
    public AbstractPlayerAction getAction() {
        return this.action;
    }

    public void setAction(@NotNull AbstractPlayerAction action) {
        this.action = action;
    }

    public void stop() {
        this.setAction(new StopAction(this.fakePlayer));
    }
}
