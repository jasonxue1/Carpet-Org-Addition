package org.carpetorgaddition.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.TextBuilder;
import org.carpetorgaddition.util.wheel.Translation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class TextUtils {
    private TextUtils() {
    }

    /**
     * 获取一个可以单击并在聊天框输入文本的可变文本组件
     *
     * @param text      原始文本，直接显示在聊天页面上
     * @param input     点击后输入在聊天框里的文本
     * @param hoverText 光标放在原始文本上显示的内容，如果为null，不显示悬停文本
     * @param color     文本的颜色，如果为null，默认为白色
     */
    public static MutableText suggest(@NotNull MutableText text, @Nullable String input, @Nullable Text hoverText, @Nullable Formatting color) {
        if (input != null) {
            //添加单击事件
            text.styled(style -> style.withClickEvent(new ClickEvent.SuggestCommand(input)));
        }
        if (hoverText != null) {
            //添加鼠标悬停事件
            text.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(hoverText)));
        }
        if (color != null) {
            text.styled(style -> style.withColor(color));
        }
        return text;
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
            text.styled(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(copy)));
        }
        if (hoverText != null) {
            text.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(hoverText)));
        }
        if (color != null) {
            text.styled(style -> style.withColor(color));
        }
        return text;
    }

    public static MutableText command(@NotNull MutableText text, @NotNull String command, @Nullable Text hoverText) {
        return command(text, command, hoverText, null, false);
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
        text.styled(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
        if (hoverText != null) {
            text.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(hoverText)));
        }
        if (color != null) {
            text.styled(style -> style.withColor(color));
        }
        return text;
    }

    /**
     * 获取一个带有悬浮文本的可变文本对象
     *
     * @param text  要显示的文本
     * @param hover 显示在文本上的悬浮文字
     */
    public static MutableText hoverText(String text, String hover) {
        return Text.literal(text).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.literal(hover))));
    }

    public static MutableText hoverText(MutableText text, String hover) {
        return hoverText(text, TextUtils.createText(hover), null);
    }

    public static MutableText hoverText(String str, Text hover) {
        return createText(str).styled(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));
    }

    public static MutableText hoverText(Text text, Text hover) {
        return hoverText((text instanceof MutableText ? (MutableText) text : text.copy()), hover, null);
    }

    public static MutableText hoverText(MutableText initialText, Text hover, @Nullable Formatting color) {
        initialText.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));
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
     * 创建一个不包含任何内容的可变文本对象
     */
    public static MutableText createEmpty() {
        return Text.literal("");
    }

    /**
     * 将一个可变文本对象设置为斜体
     */
    public static MutableText toItalic(MutableText mutableText) {
        return mutableText.styled(style -> style.withItalic(true));
    }

    /**
     * 将一个可变文本对象设置为粗体
     */
    public static MutableText toBold(MutableText mutableText) {
        return mutableText.styled(style -> style.withBold(true));
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

    /**
     * 为一段文本添加删除线
     */
    public static MutableText toStrikethrough(MutableText mutableText) {
        return mutableText.styled(style -> style.withStrikethrough(true));
    }

    /**
     * 将一堆零散的字符串和可变文本拼接成一个大的可变文本
     *
     * @param args 要拼接的文本，可以是字符串，也可以是文本，但不能是其他类型，否则抛出非法参数异常
     * @return 拼接后的可变文本对象
     */
    public static MutableText appendAll(Object... args) {
        TextBuilder textBuilder = new TextBuilder();
        for (Object obj : args) {
            switch (obj) {
                case String str -> textBuilder.appendString(str);
                case Text text -> textBuilder.append(text);
                case Number number -> textBuilder.appendTranslate(String.valueOf(number));
                case null -> throw new NullPointerException();
                default -> throw new IllegalArgumentException(obj + "即不是可变文本对象，也不是字符串对象");
            }
        }
        return textBuilder.toLine();
    }

    /**
     * 将一个集合的文本对象拼接起来，每个元素之间换行符分割
     *
     * @return 拼接后的文本对象
     */
    public static MutableText appendList(List<? extends Text> list) {
        MutableText result = createEmpty();
        for (int i = 0; i < list.size(); i++) {
            result.append(list.get(i));
            if (i < list.size() - 1) {
                result.append(TextProvider.NEW_LINE);
            }
        }
        return result;
    }

    /**
     * 获取一个可翻译文本对象
     *
     * @param key 翻译键
     * @return 可翻译文本
     */
    public static MutableText translate(String key, Object... obj) {
        String value = Translation.getTranslateValue(key);
        return Text.translatableWithFallback(key, value, obj);
    }
}
