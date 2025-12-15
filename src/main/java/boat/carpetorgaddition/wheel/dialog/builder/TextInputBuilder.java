package boat.carpetorgaddition.wheel.dialog.builder;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.wheel.TextBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.input.TextInput;

import java.util.Optional;

public class TextInputBuilder {
    private final String key;
    private int width = 200;
    private int height = -1;
    private Component label = TextBuilder.translate("carpet.generic.type");
    private boolean labelVisible = false;
    private String initial = "";
    private int maxLength = 32;
    private int maxLines = -1;
    private static final String PREFIX = CarpetOrgAddition.MOD_ID.replace('-', '_') + "_";

    private TextInputBuilder(String key) {
        int length = key.length();
        if (length > 32) {
            throw new IllegalArgumentException("The key is too long: " + key);
        }
        for (int i = 0; i < length; i++) {
            char c = key.charAt(i);
            if ((c >= 'a' && c <= 'z') || c == '_') {
                continue;
            }
            throw new IllegalArgumentException("Invalid key: %s, can only contain lowercase letters and underscores".formatted(key));
        }
        this.key = PREFIX + key;
    }

    public TextInputBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    public TextInputBuilder setLabel(Component label) {
        this.label = label;
        return this;
    }

    public TextInputBuilder setLabelVisible(boolean labelVisible) {
        this.labelVisible = labelVisible;
        return this;
    }

    public TextInputBuilder setInitial(String initial) {
        this.initial = initial;
        return this;
    }

    public TextInputBuilder setMaxLength(int maxLength) {
        this.maxLength = maxLength;
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
