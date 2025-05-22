package org.carpetorgaddition.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Deprecated(forRemoval = true)
public class TextUtils {
    private TextUtils() {
    }

    /**
     * 获取一个可以单击复制指定字符串内容到剪贴板的可变文本组件
     *
     * @param original  原始的文本，直接显示在聊天栏中的文本
     * @param copy      单击后要复制的内容
     * @param hoverText 悬停在原始文本上的内容
     * @param color     文本的颜色
     * @return 可以单击复制内容的可变文本组件
     */
    public static MutableText copy(@NotNull String original, @Nullable String copy, @Nullable Text hoverText, @Nullable Formatting color) {
        MutableText text = Text.literal(original);
        if (copy != null) {
            text.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copy)));
        }
        if (hoverText != null) {
            text.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
        }
        if (color != null) {
            text.styled(style -> style.withColor(color));
        }
        return text;
    }

    public static MutableText command(@NotNull MutableText text, @NotNull String command, @Nullable Text hoverText, @Nullable Formatting color) {
        return command(text, command, hoverText, color, false);
    }

    /**
     * 获取一个可以单击执行命令的可变文本组件
     *
     * @param underline 是否带有下划线
     * @param text      原始的文本，直接显示在聊天栏中的文本
     * @param command   点击后要执行的命令
     * @param hoverText 悬停在原始文本上的内容
     * @param color     文本的颜色
     * @return 可以单击执行命令的可变文本组件
     */
    public static MutableText command(@NotNull MutableText text, @NotNull String command, @Nullable Text hoverText, @Nullable Formatting color, boolean underline) {
        // 添加下划线
        text.styled(style -> style.withUnderline(underline));
        // 点击后执行命令
        text.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
        if (hoverText != null) {
            text.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText)));
        }
        if (color != null) {
            text.styled(style -> style.withColor(color));
        }
        return text;
    }

    public static MutableText hoverText(MutableText text, String hover) {
        return hoverText(text, TextUtils.createText(hover), null);
    }

    public static MutableText hoverText(String str, Text hover) {
        return createText(str).styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

    public static MutableText hoverText(Text text, Text hover) {
        return hoverText((text instanceof MutableText ? (MutableText) text : text.copy()), hover, null);
    }

    public static MutableText hoverText(MutableText initialText, Text hover, @Nullable Formatting color) {
        initialText.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        if (color != null) {
            initialText.styled(style -> style.withColor(color));
        }
        return initialText;
    }

    /**
     * 根据字符串创建一个新的可变文本对象
     *
     * @param text 可变文本对象的内容
     * @return 一个新的不包含任何样式的可变文本对象
     */
    public static MutableText createText(String text) {
        return Text.literal(text);
    }

    public static MutableText createText(Number number) {
        return Text.literal(number.toString());
    }

    /**
     * 将一个可变文本对象设置为斜体
     */
    public static MutableText toItalic(MutableText mutableText) {
        return mutableText.styled(style -> style.withItalic(true));
    }

    /**
     * 设置一个可变文本对象的颜色
     */
    public static MutableText setColor(MutableText mutableText, Formatting formatting) {
        return mutableText.styled(style -> style.withColor(formatting));
    }

    /**
     * 将一个文本对象设置为灰色斜体
     */
    public static MutableText toGrayItalic(MutableText mutableText) {
        return toItalic(setColor(mutableText, Formatting.GRAY));
    }
}
