package org.carpetorgaddition.wheel;

import com.mojang.brigadier.Message;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public class TextBuilder {
    @NotNull
    private MutableText text;

    public TextBuilder() {
        this(empty());
    }

    public TextBuilder(Text text) {
        this(text.copy());
    }

    public TextBuilder(@NotNull MutableText text) {
        this.text = text;
    }

    public TextBuilder(String str) {
        this(create(str));
    }

    public TextBuilder(Number number) {
        this(number.toString());
    }

    public TextBuilder(Message message) {
        this(create(message));
    }

    public static TextBuilder of(String key, Object... args) {
        return new TextBuilder(translate(key, args));
    }

    public static TextBuilder fromCombined(Object... args) {
        return new TextBuilder(combineAll(args));
    }

    public static MutableText empty() {
        return Text.empty();
    }

    public static MutableText create(String str) {
        return Text.literal(str);
    }

    public static MutableText create(Number number) {
        return Text.literal(number.toString());
    }

    public static MutableText create(Message message) {
        return Text.of(message).copy();
    }

    /**
     * 设置文本单击后在聊天栏输入内容
     */
    public TextBuilder setSuggest(String input) {
        this.text.styled(style -> style.withClickEvent(new ClickEvent.SuggestCommand(input)));
        return this;
    }

    /**
     * 设置文本颜色
     */
    public TextBuilder setColor(Formatting color) {
        if (color == null) {
            return this;
        }
        this.text.styled(style -> style.withColor(color));
        return this;
    }

    /**
     * 设置悬停提示
     */
    public TextBuilder setHover(Text text) {
        this.text.styled(style -> style.withHoverEvent(new HoverEvent.ShowText(text)));
        return this;
    }

    public TextBuilder setHover(MetaComment comment) {
        if (comment.hasContent()) {
            this.setHover(comment.getText());
        }
        return this;
    }

    public TextBuilder setHover(String key, Object... args) {
        return this.setHover(translate(key, args));
    }

    public TextBuilder setHover(Throwable e) {
        String error = GenericUtils.getExceptionString(e);
        return this.setHover(create(error));
    }

    public TextBuilder setStringHover(String hover) {
        return this.setHover(create(hover));
    }

    /**
     * 设置单击文本后复制内容到剪贴板
     */
    public TextBuilder setCopyToClipboard(String str) {
        this.text.styled(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(str)));
        this.setHover(TextProvider.COPY_CLICK);
        return this;
    }

    /**
     * 设置单击文本后执行命令
     */
    public TextBuilder setCommand(String command) {
        this.text.styled(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
        return this;
    }

    /**
     * 设置单击文本后聊天框输入命令
     */
    public TextBuilder setSuggestCommand(String command) {
        this.text.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
        return this;
    }

    /**
     * 设置为斜体
     */
    public TextBuilder setItalic() {
        this.text.styled(style -> style.withItalic(true));
        return this;
    }

    /**
     * 设置为粗体
     */
    public TextBuilder setBold() {
        this.text.styled(style -> style.withBold(true));
        return this;
    }

    /**
     * 设置有删除线
     */
    public TextBuilder setStrikethrough() {
        this.text.styled(style -> style.withStrikethrough(true));
        return this;
    }

    /**
     * 设置有下划线
     */
    public TextBuilder setUnderline() {
        this.text.styled(style -> style.withUnderline(true));
        return this;
    }

    /**
     * 设置为灰色斜体
     */
    public TextBuilder setGrayItalic() {
        return this.setColor(Formatting.GRAY).setItalic();
    }

    public TextBuilder append(String str) {
        return this.append(create(str));
    }

    public TextBuilder append(@Nullable Text text) {
        if (text == null) {
            return this;
        }
        this.text = empty().append(this.text).append(text);
        return this;
    }

    public TextBuilder append(TextBuilder builder) {
        return this.append(builder.text);
    }

    public MutableText build() {
        return this.text;
    }

    /**
     * 将一堆零散的数据拼接成一个大的{@code MutableText}
     *
     * @param args 要拼接的文本
     * @return 拼接后的 {@code MutableText}对象
     */
    public static MutableText combineAll(Object... args) {
        MutableText result = empty();
        for (Object obj : args) {
            appendEach(obj, result);
        }
        return result;
    }

    public static MutableText combineList(List<?> list) {
        MutableText result = empty();
        list.forEach(obj -> appendEach(obj, result));
        return result;
    }

    private static void appendEach(@Nullable Object obj, MutableText result) {
        switch (obj) {
            case String str -> result.append(str);
            case Text text -> result.append(text);
            case Message message -> result.append(create(message));
            case Number number -> result.append(String.valueOf(number));
            case TextBuilder builder -> result.append(builder.text);
            case null -> {
            }
            // 译：%s不可解析为Text类型
            default -> throw new IllegalArgumentException(obj + " cannot be parsed as a Text type");
        }
    }

    /**
     * 将一个集合的文本对象拼接起来，每个元素之间换行符分割
     *
     * @return 拼接后的文本对象
     */
    public static MutableText joinList(List<? extends Text> list) {
        return joinList(list, TextProvider.NEW_LINE);
    }

    /**
     * 将一个集合的文本对象拼接起来
     *
     * @return 拼接后的文本对象
     */
    public static MutableText joinList(List<? extends Text> list, Text separator) {
        MutableText result = empty();
        for (int i = 0; i < list.size(); i++) {
            result.append(list.get(i));
            if (i < list.size() - 1) {
                result.append(separator);
            }
        }
        return result;
    }

    /**
     * 获取一个可翻译文本对象
     *
     * @param key 翻译键
     * @return 可翻译文本
     * @apiNote 客户端不需要有对应的翻译
     */
    public static MutableText translate(String key, Object... obj) {
        String value = Translation.getTranslateValue(key);
        return Text.translatableWithFallback(key, value, obj);
    }
}
