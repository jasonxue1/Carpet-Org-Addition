package org.carpetorgaddition.util.wheel;

import com.mojang.brigadier.Message;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.util.TextUtils;
import org.jetbrains.annotations.Nullable;

public class TextBuilder {
    private final MutableText text;

    private TextBuilder(MutableText text) {
        this.text = text;
    }

    public static TextBuilder of(@Nullable Object obj) {
        MutableText result = switch (obj) {
            case null -> TextUtils.createEmpty();
            case String str -> TextUtils.createText(str);
            case Number number -> TextUtils.createText(number);
            case MutableText text -> text;
            case Text text -> text.copy();
            case Message message -> TextUtils.create(message).copy();
            default -> throw new IllegalArgumentException(obj + " cannot be parsed as a Text type");
        };
        return new TextBuilder(result);
    }

    public static TextBuilder ofTranslate(String key, Object... args) {
        return new TextBuilder(TextUtils.translate(key, args));
    }

    /**
     * 设置文本单击后在聊天栏输入内容
     */
    public TextBuilder setSuggest(String input) {
        this.text.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, input)));
        return this;
    }

    /**
     * 设置文本颜色
     */
    public TextBuilder setColor(Formatting formatting) {
        this.text.styled(style -> style.withColor(formatting));
        return this;
    }

    /**
     * 设置悬停提示
     */
    public TextBuilder setHover(Text text) {
        this.text.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text)));
        return this;
    }

    /**
     * 设置悬停提示
     */
    public TextBuilder setHover(String key, Object... args) {
        return this.setHover(TextUtils.translate(key, args));
    }

    /**
     * 设置单击文本后复制内容到剪贴板
     */
    public TextBuilder setCopyToClipboard(String str) {
        this.text.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, str)));
        return this;
    }

    /**
     * 设置单击文本后执行命令
     */
    public TextBuilder setCommand(String command) {
        this.text.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
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
     * 设置为灰色斜体
     */
    public TextBuilder setGrayItalic() {
        return this.setColor(Formatting.GRAY).setItalic();
    }

    public MutableText build() {
        return this.text;
    }
}
