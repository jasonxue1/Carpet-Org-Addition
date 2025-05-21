package org.carpetorgaddition.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.Objects;

public class ClientMessageUtils {
    private ClientMessageUtils() {
    }

    /**
     * 向客户端玩家发送一条聊天消息
     */
    public static void sendMessage(Text message) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("尝试在游戏外发送聊天消息");
            return;
        }
        player.sendMessage(message);
    }

    /**
     * 向客户端玩家发送一条聊天消息
     */
    public static void sendMessage(String key, Object... args) {
        sendMessage(TextUtils.translate(key, args));
    }

    /**
     * 向客户端玩家发送一条红色的聊天消息
     */
    public static void sendErrorMessage(Text message) {
        sendMessage(TextBuilder.of(message).setColor(Formatting.RED).build());
    }

    public static void sendErrorMessage(Throwable e, String key, Object... args) {
        // TODO 修改异常消息内容
        TextBuilder builder = TextBuilder.ofTranslate(key, args);
        builder.setHover(Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()));
        sendErrorMessage(builder.build());
    }
}
