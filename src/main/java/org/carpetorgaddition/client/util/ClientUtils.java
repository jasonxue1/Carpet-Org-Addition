package org.carpetorgaddition.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ClientUtils {
    private ClientUtils() {
    }

    /**
     * 获取游戏客户端示例
     */
    public static MinecraftClient getClient() {
        return MinecraftClient.getInstance();
    }

    /**
     * 获取集成服务器
     */
    @Nullable
    @Contract(pure = true)
    public static IntegratedServer getServer() {
        return getClient().getServer();
    }

    /**
     * 获取客户端玩家
     */
    @NotNull
    @Contract(pure = true)
    public static ClientPlayerEntity getPlayer() {
        ClientPlayerEntity player = getClient().player;
        if (player == null) {
            throw new IllegalStateException("Attempted to get client player while not in a game");
        }
        return player;
    }

    @SuppressWarnings("unused")
    public static Optional<Entity> getEntity(int id) {
        if (id == -1) {
            return Optional.empty();
        }
        return Optional.ofNullable(getWorld().getEntityById(id));
    }

    /**
     * 获取玩家所处客户端世界
     */
    @NotNull
    @Contract(pure = true)
    public static ClientWorld getWorld() {
        ClientWorld world = getClient().world;
        if (world == null) {
            throw new IllegalStateException("Attempted to get client world while not in a game");
        }
        return world;
    }

    /**
     * @return 获取游戏渲染器
     */
    @NotNull
    public static GameRenderer getGameRenderer() {
        return getClient().gameRenderer;
    }

    /**
     * 获取文字渲染器
     */
    public static TextRenderer getTextRenderer() {
        return getClient().textRenderer;
    }

    /**
     * 获取摄像机
     */
    @Contract(pure = true)
    public static Camera getCamera() {
        return getGameRenderer().getCamera();
    }

    /**
     * 获取当前打开的屏幕
     */
    @Nullable
    public static Screen getCurrentScreen() {
        return getClient().currentScreen;
    }

    /**
     * 获取客户端游戏设置
     */
    public static GameOptions getGameOptions() {
        return getClient().options;
    }

    /**
     * 获取当前的准星指向
     */
    @Nullable
    public static HitResult getCrosshairTarget() {
        return getClient().crosshairTarget;
    }

    /**
     * 获取鼠标信息
     */
    public static Mouse getMouse() {
        return getClient().mouse;
    }
}
