package boat.carpetorgaddition.wheel.text;

import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.wheel.MetaComment;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import com.mojang.brigadier.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class TextBuilder {
    @NotNull
    private MutableComponent text;

    private TextBuilder(@NotNull MutableComponent text) {
        this.text = text;
    }

    public TextBuilder() {
        this(empty());
    }

    public TextBuilder(Component text) {
        this(text.copy());
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

    @Deprecated
    public static TextBuilder of(String key, Object... args) {
        return new TextBuilder(translate(key, args));
    }

    public static TextBuilder of(LocalizationKey key, Object... args) {
        return new TextBuilder(translate(key, args));
    }

    public static TextBuilder fromCombined(Object... args) {
        return new TextBuilder(combineAll(args));
    }

    public static Component empty() {
        return Component.empty();
    }

    public static Component create(String str) {
        return Component.literal(str);
    }

    public static Component create(Number number) {
        return Component.literal(number.toString());
    }

    public static Component create(Message message) {
        return Component.translationArg(message).copy();
    }

    public static Component create(Identifier identifier) {
        return TextBuilder.create(identifier.toString());
    }

    /**
     * 设置文本单击后在聊天栏输入内容
     */
    public TextBuilder setSuggest(String input) {
        this.text.withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand(input)));
        return this;
    }

    /**
     * 设置文本颜色
     */
    public TextBuilder setColor(ChatFormatting color) {
        if (color == null) {
            return this;
        }
        this.text.withStyle(style -> style.withColor(color));
        return this;
    }

    /**
     * 设置悬停提示
     */
    public TextBuilder setHover(Component text) {
        this.text.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(text)));
        return this;
    }

    public TextBuilder setHover(MetaComment comment) {
        if (comment.hasContent()) {
            this.setHover(comment.getText());
        }
        return this;
    }

    @Deprecated
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
        return setCopyToClipboard(str, true);
    }

    /**
     * 设置单击文本后复制内容到剪贴板
     *
     * @param hover 是否显示“单击复制到剪贴板”的悬停提示
     */
    public TextBuilder setCopyToClipboard(String str, boolean hover) {
        this.text.withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(str)));
        if (hover) {
            this.setHover(TextProvider.COPY_CLICK);
        }
        return this;
    }

    /**
     * 设置单击文本后执行命令
     */
    public TextBuilder setCommand(String command) {
        this.text.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
        return this;
    }

    /**
     * 设置自定义单击动作
     */
    public TextBuilder setCustomEvent(Identifier identifier, NbtWriter writer) {
        writer.putActionSource(ActionSource.CHAT);
        this.text.withStyle(style -> style.withClickEvent(new ClickEvent.Custom(identifier, Optional.of(writer.toNbt()))));
        return this;
    }

    /**
     * 设置单击文本后聊天框输入命令
     */
    public TextBuilder setSuggestCommand(String command) {
        this.text.withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand(command)));
        return this;
    }

    /**
     * 设置为斜体
     */
    public TextBuilder setItalic() {
        this.text.withStyle(style -> style.withItalic(true));
        return this;
    }

    /**
     * 设置为粗体
     */
    public TextBuilder setBold() {
        this.text.withStyle(style -> style.withBold(true));
        return this;
    }

    /**
     * 设置有删除线
     */
    public TextBuilder setStrikethrough() {
        this.text.withStyle(style -> style.withStrikethrough(true));
        return this;
    }

    /**
     * 设置有下划线
     */
    public TextBuilder setUnderline() {
        this.text.withStyle(style -> style.withUnderlined(true));
        return this;
    }

    /**
     * 设置为随机字符
     */
    public TextBuilder setObfuscated() {
        this.text.withStyle(style -> style.withObfuscated(true));
        return this;
    }

    /**
     * 设置为灰色斜体
     */
    public TextBuilder setGrayItalic() {
        return this.setColor(ChatFormatting.GRAY).setItalic();
    }

    public TextBuilder append(String str) {
        return this.append(create(str));
    }

    public TextBuilder append(@Nullable Component text) {
        if (text == null) {
            return this;
        }
        this.text = empty().copy().append(this.text).append(text);
        return this;
    }

    public TextBuilder append(TextBuilder builder) {
        return this.append(builder.text);
    }

    public Component build() {
        return this.text;
    }

    /**
     * 将一堆零散的数据拼接成一个大的{@code MutableText}
     *
     * @param args 要拼接的文本
     * @return 拼接后的 {@code MutableText}对象
     */
    public static Component combineAll(Object... args) {
        MutableComponent result = empty().copy();
        for (Object obj : args) {
            appendEach(obj, result);
        }
        return result;
    }

    public static Component combineList(List<?> list) {
        MutableComponent result = empty().copy();
        list.forEach(obj -> appendEach(obj, result));
        return result;
    }

    private static void appendEach(@Nullable Object obj, MutableComponent result) {
        switch (obj) {
            case String str -> result.append(str);
            case Component text -> result.append(text);
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
    public static Component joinList(List<? extends Component> list) {
        return joinList(list, TextProvider.NEW_LINE);
    }

    /**
     * 将一个集合的文本对象拼接起来
     *
     * @return 拼接后的文本对象
     */
    public static Component joinList(List<? extends Component> list, Component separator) {
        MutableComponent result = empty().copy();
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
    @Deprecated
    public static Component translate(String key, Object... obj) {
        String value = Translation.getTranslateValue(key);
        return Component.translatableWithFallback(key, value, obj);
    }

    @Deprecated
    public static Component translate(LocalizationKey local, Object... obj) {
        String key = local.toString();
        String value = Translation.getTranslateValue(key);
        return Component.translatableWithFallback(key, value, obj);
    }
}
