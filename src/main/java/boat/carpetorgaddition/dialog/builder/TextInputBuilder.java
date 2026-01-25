package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.dialog.DialogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.input.TextInput;

import java.util.Optional;

public class TextInputBuilder {
    private final String key;
    private int width = 200;
    private int height = -1;
    private Component label = DialogUtils.UNDEFINED;
    private boolean labelVisible = true;
    private String initial = "";
    private int maxLength = 32;
    private int maxLines = -1;

    private TextInputBuilder(String key) {
        this.key = DialogUtils.toValidDialogKey(key);
    }

    public static TextInputBuilder of(String key) {
        return new TextInputBuilder(key);
    }

    @SuppressWarnings("unused")
    public TextInputBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    public TextInputBuilder setLabel(Component label) {
        this.label = label;
        return this;
    }

    @SuppressWarnings("unused")
    public TextInputBuilder setLabelVisible(boolean labelVisible) {
        this.labelVisible = labelVisible;
        return this;
    }

    @SuppressWarnings("unused")
    public TextInputBuilder setInitial(String initial) {
        this.initial = initial;
        return this;
    }

    public TextInputBuilder setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    @SuppressWarnings("unused")
    public TextInputBuilder setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException();
        }
        this.height = height;
        return this;
    }

    @SuppressWarnings("unused")
    public TextInputBuilder setMaxLines(int maxLines) {
        if (maxLines <= 0) {
            throw new IllegalArgumentException();
        }
        this.maxLines = maxLines;
        return this;
    }

    public Input build() {
        Optional<TextInput.MultilineOptions> optional;
        if (this.height == -1 && this.maxLines == -1) {
            optional = Optional.empty();
        } else {
            Optional<Integer> maxLines = this.maxLines == -1 ? Optional.empty() : Optional.of(this.maxLines);
            Optional<Integer> height = this.height == -1 ? Optional.empty() : Optional.of(this.height);
            TextInput.MultilineOptions options = new TextInput.MultilineOptions(maxLines, height);
            optional = Optional.of(options);
        }
        return new Input(this.key, new TextInput(this.width, this.label, this.labelVisible, this.initial, this.maxLength, optional));
    }
}
