package org.carpetorgaddition.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ClientUtils {
    @NotNull
    public static ClientWorld getWorld() {
        return Objects.requireNonNull(MinecraftClient.getInstance().world);
    }

    @NotNull
    public static ClientPlayerEntity getPlayer() {
        return Objects.requireNonNull(MinecraftClient.getInstance().player);
    }

    @NotNull
    public static GameRenderer getGameRenderer() {
        return MinecraftClient.getInstance().gameRenderer;
    }

    public static Camera getCamera() {
        return getGameRenderer().getCamera();
    }
}
