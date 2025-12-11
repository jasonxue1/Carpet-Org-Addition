package org.carpetorgaddition.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.carpetorgaddition.client.util.ClientUtils;

import java.util.List;

public class Tooltip {
    private Tooltip() {
    }

    /**
     * 在屏幕中心偏右下的位置渲染一个提示框
     *
     * @param context 绘制上下文
     * @param list    提示框的内容，一个元素表示提示内的一行文本
     */
    public static void drawTooltip(GuiGraphics context, List<Component> list) {
        Minecraft client = ClientUtils.getClient();
        int height = client.getWindow().getGuiScaledHeight();
        int width = client.getWindow().getGuiScaledWidth();
        context.setComponentTooltipForNextFrame(ClientUtils.getTextRenderer(), list, width / 2 + 7, height / 2 + 27);
    }

    public static void drawTooltip(GuiGraphics context, Component text) {
        drawTooltip(context, List.of(text));
    }
}
