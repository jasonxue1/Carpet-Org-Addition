package org.carpetorgaddition.periodic.task.schedule;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.Messenger;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.jetbrains.annotations.Contract;

public class ReLoginTask extends PlayerScheduleTask {
    public static final ThreadLocal<Boolean> HOME_POSITION = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> INTERNAL_HOME_POSITION = ThreadLocal.withInitial(() -> false);
    // 假玩家名
    private final FakePlayerSerializer serializer;
    // 重新上线的时间间隔
    private int interval;
    // 距离下一次重新上线所需的时间
    private int remainingTick;
    private final MinecraftServer server;
    private final ServerCommandSource source;
    // 当前任务是否已经结束
    private boolean stop = false;
    // 假玩家重新上线的倒计时
    private int canSpawn = 2;

    public ReLoginTask(EntityPlayerMPFake fakePlayer, int interval, MinecraftServer server, ServerCommandSource source) {
        this.serializer = new FakePlayerSerializer(fakePlayer);
        this.interval = interval;
        this.remainingTick = this.interval;
        this.server = server;
        this.source = source;
    }

    @Override
    public void tick() {
        // 启用内存泄漏修复
        if (CarpetOrgAdditionSettings.fakePlayerSpawnMemoryLeakFix.get()) {
            ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.getPlayerName());
            if (player == null) {
                if (this.canSpawn == 0) {
                    loginPlayer();
                    this.canSpawn = 2;
                } else {
                    this.canSpawn--;
                }
            } else if (this.remainingTick <= 0) {
                this.remainingTick = this.interval;
                if (player instanceof EntityPlayerMPFake fakePlayer) {
                    // 如果假玩家坠入虚空，设置任务为停止
                    if (fakePlayer.getY() < fakePlayer.getServerWorld().getBottomY() - 64) {
                        this.stop();
                    }
                    // 让假玩家退出游戏
                    logoutPlayer(fakePlayer);
                }
            } else {
                this.remainingTick--;
            }
        } else {
            Runnable function = () -> {
                MessageUtils.sendErrorMessage(source, "carpet.commands.playerManager.schedule.relogin.rule.disable");
                // 如果假玩家已经下线，重新生成假玩家
                ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.getPlayerName());
                if (player == null) {
                    loginPlayer();
                }
            };
            throw new TaskExecutionException(function);
        }
    }


    /**
     * 让假玩家退出游戏
     *
     * @see EntityPlayerMPFake#kill(Text)
     * @see EntityPlayerMPFake#shakeOff()
     */
    @SuppressWarnings("JavadocReference")
    public static void logoutPlayer(EntityPlayerMPFake fakePlayer) {
        Text reason = Messenger.s("Killed");
        // 停止骑行
        if (fakePlayer.getVehicle() instanceof PlayerEntity) {
            fakePlayer.stopRiding();
        }
        for (Entity passenger : fakePlayer.getPassengersDeep()) {
            if (passenger instanceof PlayerEntity) {
                passenger.stopRiding();
            }
        }
        // 退出游戏
        TextContent content = reason.getContent();
        if (content instanceof TranslatableTextContent text) {
            if (text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
                try {
                    CarpetOrgAdditionSettings.hiddenLoginMessages.setExternal(true);
                    fakePlayer.networkHandler.onDisconnected(new DisconnectionInfo(reason));
                } finally {
                    CarpetOrgAdditionSettings.hiddenLoginMessages.setExternal(false);
                }
                return;
            }
        }
        MinecraftServer server = GenericUtils.getServer(fakePlayer);
        server.send(new ServerTask(server.getTicks(), () -> {
            try {
                CarpetOrgAdditionSettings.hiddenLoginMessages.setExternal(true);
                /*
                 * 如果不加这个判断并提前返回，可能导致玩家的骑乘实体消失，可能的原因如下：
                 * 1. 玩家在下线后会保存一次数据，其中包括了当前骑乘的实体，下一次上线时，游戏就会从NBT中读取并生成骑乘实体。
                 * 2. 保存完自身的实体数据后就会从正在骑乘的实体身上下来，这时如果再获取这个玩家的骑乘实体就会返回null。
                 * 3. 接下来，如果这个玩家再触发一次保存，就会将null值写入玩家的骑乘实体，或者说，玩家就丢失了骑乘实体的数据。
                 * 4. 再次上线就无法重新生成之前的骑乘实体，因此，如果这个玩家已经被删除，就不能让该玩家再次触发保存了。
                 * 无法验证这样做是否能完全避免骑乘实体消失，也并不知道问题是不是真的由这个原因引起的，但这至少能保证在执行命令/tick sprint <time>时实体不会消失。
                 * 如果真的是因为不正确的玩家数据保存，那么保存是如何触发的，是服务器的自动保存吗？为什么执行/tick sprint <time>也会导致骑乘实体消失？
                 */
                if (fakePlayer.isRemoved()) {
                    return;
                }
                fakePlayer.networkHandler.onDisconnected(new DisconnectionInfo(reason));
            } finally {
                CarpetOrgAdditionSettings.hiddenLoginMessages.setExternal(false);
            }
        }));
    }

    @Override
    public boolean stopped() {
        return this.stop;
    }

    @Override
    public String getLogName() {
        return this.getPlayerName() + "周期性重新上线";
    }

    @Override
    @Contract(pure = true)
    public String getPlayerName() {
        return this.serializer.getFakePlayerName();
    }

    @Override
    public void onCancel(CommandContext<ServerCommandSource> context) {
        this.markRemove();
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.relogin.cancel", this.getPlayerName());
        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.getPlayerName());
        if (player == null) {
            loginPlayer();
        }
    }

    @Override
    public void sendEachMessage(ServerCommandSource source) {
        MessageUtils.sendMessage(source, "carpet.commands.playerManager.schedule.relogin", this.getPlayerName(), this.interval);
    }

    public void setInterval(int interval) {
        this.interval = interval;
        this.remainingTick = interval;
    }

    public void stop() {
        this.stop = true;
    }

    /**
     * 生成假玩家
     */
    private void loginPlayer() {
        try {
            CarpetOrgAdditionSettings.hiddenLoginMessages.setExternal(true);
            try {
                HOME_POSITION.set(true);
                this.serializer.spawn(this.server);
            } finally {
                HOME_POSITION.set(false);
            }
        } catch (CommandSyntaxException e) {
            CommandUtils.handlingException(e, this.source);
            this.stop();
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.warn("Fake player encounter unexpected errors while logging in", e);
            this.stop();
        } finally {
            CarpetOrgAdditionSettings.hiddenLoginMessages.setExternal(false);
        }
    }
}
