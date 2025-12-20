package boat.carpetorgaddition.client.util;

import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class ClientMessageUtils {
    private ClientMessageUtils() {
    }

    /**
     * 向客户端玩家发送一条聊天消息
     */
    public static void sendMessage(Component message) {
        LocalPlayer player = ClientUtils.getPlayer();
        player.displayClientMessage(message, false);
    }

    /**
     * 向客户端玩家发送一条聊天消息
     */
    @Deprecated
    public static void sendMessage(String key, Object... args) {
        sendMessage(TextBuilder.translate(key, args));
    }

    /**
     * 向客户端玩家发送一条红色的聊天消息
     */
    public static void sendErrorMessage(Component message) {
        sendMessage(new TextBuilder(message).setColor(ChatFormatting.RED).build());
    }

    @Deprecated
    public static void sendErrorMessage(Throwable e, String key, Object... args) {
        String error = GenericUtils.getExceptionString(e);
        TextBuilder builder = TextBuilder.of(key, args);
        builder.setStringHover(error);
        builder.setColor(ChatFormatting.RED);
        sendErrorMessage(builder.build());
    }
}
