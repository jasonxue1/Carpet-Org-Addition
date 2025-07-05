package org.carpetorgaddition.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.TextBuilder;

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
        sendMessage(TextBuilder.translate(key, args));
    }

    /**
     * 向客户端玩家发送一条红色的聊天消息
     */
    public static void sendErrorMessage(Text message) {
        sendMessage(new TextBuilder(message).setColor(Formatting.RED).build());
    }

    public static void sendErrorMessage(Throwable e, String key, Object... args) {
        String error = GenericUtils.getExceptionString(e);
        TextBuilder builder = TextBuilder.of(key, args);
        builder.setStringHover(error);
        builder.setColor(Formatting.RED);
        sendErrorMessage(builder.build());
    }
}
