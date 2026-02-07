package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.body.PlainMessage;

@SuppressWarnings("unused")
public class PlainMessageBuilder {
    /**
     * 要渲染的文本内容
     */
    private Component contents = TextBuilder.empty();
    /**
     * 文本区域的宽度
     */
    private int width = PlainMessage.DEFAULT_WIDTH;

    private PlainMessageBuilder() {
    }

    public static PlainMessageBuilder of() {
        return new PlainMessageBuilder();
    }

    public PlainMessageBuilder setContents(Component contents) {
        this.contents = contents;
        return this;
    }

    public PlainMessageBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    public PlainMessage build() {
        return new PlainMessage(this.contents, this.width);
    }
}
