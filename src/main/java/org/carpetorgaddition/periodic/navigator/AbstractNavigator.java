package org.carpetorgaddition.periodic.navigator;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import org.carpetorgaddition.periodic.PlayerComponentCoordinator;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNavigator {
    protected static final String IN = "carpet.commands.navigate.hud.in";
    protected static final String DISTANCE = "carpet.commands.navigate.hud.distance";
    protected static final String REACH = "carpet.commands.navigate.hud.reach";
    protected final ServerPlayerEntity player;
    protected final MinecraftServer server;
    protected final NavigatorManager manager;

    public AbstractNavigator(ServerPlayerEntity player) {
        this.player = player;
        this.server = FetcherUtils.getServer(player);
        this.manager = PlayerComponentCoordinator.getManager(this.player).getNavigatorManager();
    }

    /**
     * 开始导航时调用
     */
    public void onStart() {
        this.syncWaypoint(true);
    }

    /**
     * 每个游戏刻都调用
     */
    public abstract void tick();

    /**
     * 此导航器的结束条件
     *
     * @return 导航是否需要结束
     */
    protected abstract boolean isArrive();

    /**
     * @return 此导航器的浅拷贝副本
     */
    public abstract AbstractNavigator copy(ServerPlayerEntity player);

    @NotNull
    protected Text getHUDText(Vec3d vec3d, Text in, Text distance) {
        Text text;
        // 添加上下箭头
        text = switch (verticalAngle(this.player, vec3d)) {
            case 1 -> TextBuilder.combineAll(in, " ↑ ", distance);
            case -1 -> TextBuilder.combineAll(in, " ↓ ", distance);
            default -> TextBuilder.combineAll(in, "   ", distance);
        };
        // 添加左右箭头
        text = switch (forwardAngle(this.player, vec3d)) {
            case -3 -> TextBuilder.combineAll("    ", text, " >>>");
            case -2 -> TextBuilder.combineAll("    ", text, "  >>");
            case -1 -> TextBuilder.combineAll("    ", text, "   >");
            case 1 -> TextBuilder.combineAll("<   ", text, "    ");
            case 2 -> TextBuilder.combineAll("<<  ", text, "    ");
            case 3 -> TextBuilder.combineAll("<<< ", text, "    ");
            default -> TextBuilder.combineAll("    ", text, "    ");
        };
        return text;
    }

    /**
     * @param target 玩家看向的位置
     * @see net.minecraft.client.gui.hud.SubtitlesHud#render(DrawContext)
     */
    private static int forwardAngle(PlayerEntity player, Vec3d target) {
        double x = target.getX() - player.getX();
        double y = target.getZ() - player.getZ();
        // 将直角坐标转换为极坐标，然后获取角度
        double result = player.getYaw() + Math.toDegrees(Math.atan2(x, y));
        result = result < 0 ? result + 360 : result;
        result = result > 180 ? result - 360 : result;
        return forwardAngle(result);
    }

    private static int forwardAngle(double value) {
        if (value < 0) {
            return -forwardAngle(-value);
        }
        if (value <= 3) {
            return 0;
        }
        if (value <= 60) {
            return 1;
        }
        if (value <= 100) {
            return 2;
        }
        return 3;
    }

    // 玩家视角是否指向目标位置（仅考虑高度）
    private static int verticalAngle(PlayerEntity player, Vec3d target) {
        double x = Math.sqrt(Math.pow(player.getX() - target.getX(), 2) + Math.pow(player.getZ() - target.getZ(), 2));
        double y = target.getY() - player.getEyeY();
        double result = player.getPitch() + Math.toDegrees(Math.atan2(y, x));
        if (result >= 10) {
            return 1;
        } else if (result <= -10) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * 同步路径点
     */
    public void syncWaypoint(boolean force) {
        // 更新上一个坐标
        if (force || this.updateRequired()) {
            // 要求玩家有执行/navigate命令的权限
            boolean hasPermission = CommandUtils.canUseCommand(this.player.getCommandSource(), CarpetOrgAdditionSettings.commandNavigate);
            if (CarpetOrgAdditionSettings.syncNavigateWaypoint.get() && hasPermission) {
                WaypointUpdateS2CPacket packet = this.createPacket();
                ServerPlayNetworking.send(this.player, packet);
            }
        }
    }

    /**
     * 创建同步数据包
     */
    protected abstract WaypointUpdateS2CPacket createPacket();

    /**
     * @return 坐标是否需要更新
     */
    protected abstract boolean updateRequired();

    /**
     * 让玩家清除这个导航器
     */
    public void clear() {
        this.manager.clearNavigator();
    }
}
