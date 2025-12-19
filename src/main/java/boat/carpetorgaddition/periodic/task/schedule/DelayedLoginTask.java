package boat.carpetorgaddition.periodic.task.schedule;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public class DelayedLoginTask extends PlayerScheduleTask {
    private final MinecraftServer server;
    private final FakePlayerSerializer serial;
    private long delayed;

    public DelayedLoginTask(MinecraftServer server, CommandSourceStack source, FakePlayerSerializer serial, long delayed) {
        super(source);
        this.server = server;
        this.serial = serial;
        this.delayed = delayed;
    }

    @Override
    public void tick() {
        if (this.delayed == 0L) {
            try {
                // 生成假玩家
                try {
                    serial.spawn(server);
                } catch (CommandSyntaxException e) {
                    CarpetOrgAddition.LOGGER.error("玩家{}已存在", this.serial.getFakePlayerName(), e);
                } catch (RuntimeException e) {
                    CarpetOrgAddition.LOGGER.error("玩家{}未能在指定时间上线", this.serial.getFakePlayerName(), e);
                }
            } finally {
                // 将此任务设为已执行结束
                this.delayed = -1L;
            }
        } else {
            this.delayed--;
        }
    }

    @Override
    public String getPlayerName() {
        return serial.getFakePlayerName();
    }

    @Override
    public void onCancel(CommandContext<CommandSourceStack> context) {
        this.markRemove();
        Component time = getDisplayTime();
        Component displayName = this.serial.getDisplayName();
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.login.cancel", displayName, time);
    }

    // 获取带有悬停提示的时间
    private @NotNull Component getDisplayTime() {
        TextBuilder builder = new TextBuilder(TextProvider.tickToTime(this.delayed));
        builder.setHover(TextProvider.tickToRealTime(this.delayed));
        return builder.build();
    }

    @Override
    public void sendEachMessage(CommandSourceStack source) {
        MessageUtils.sendMessage(source, "carpet.commands.playerManager.schedule.login",
                this.serial.getDisplayName(), this.getDisplayTime());
    }

    public void setDelayed(long delayed) {
        this.delayed = delayed;
    }

    public Component getInfo() {
        return this.serial.info();
    }

    @Override
    public boolean stopped() {
        return this.delayed < 0L;
    }

    @Override
    public String getLogName() {
        return this.serial.getFakePlayerName() + "延迟上线";
    }
}
