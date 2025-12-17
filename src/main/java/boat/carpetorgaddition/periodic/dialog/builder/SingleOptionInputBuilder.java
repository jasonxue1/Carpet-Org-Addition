package boat.carpetorgaddition.periodic.dialog.builder;

import boat.carpetorgaddition.periodic.dialog.DialogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.input.SingleOptionInput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SingleOptionInputBuilder {
    private final String key;
    private int width = 200;
    private final List<SingleOptionInput.Entry> entries = new ArrayList<>();
    private Component label = DialogUtils.UNDEFINED;
    private boolean labelVisible = true;

    private SingleOptionInputBuilder(String key) {
        this.key = DialogUtils.assertValidDialogKey(key);
    }

    public static SingleOptionInputBuilder of(String key) {
        return new SingleOptionInputBuilder(key);
    }

    @SuppressWarnings("unused")
    public SingleOptionInputBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    @SuppressWarnings("unused")
    public SingleOptionInputBuilder setLabel(Component label) {
        this.label = label;
        return this;
    }

    public SingleOptionInputBuilder setLabelVisible(boolean labelVisible) {
        this.labelVisible = labelVisible;
        return this;
    }

    @SuppressWarnings("unused")
    public SingleOptionInputBuilder addEntry(String id) {
        return this.addEntry(id, null);
    }

    public SingleOptionInputBuilder addEntry(String id, @Nullable Component display) {
        return this.addEntry(id, display, this.entries.isEmpty());
    }

    public SingleOptionInputBuilder addEntry(String id, @Nullable Component display, boolean initial) {
        this.entries.add(new SingleOptionInput.Entry(id, Optional.ofNullable(display), initial));
        return this;
    }

    public Input build() {
        return new Input(this.key, new SingleOptionInput(this.width, this.entries, this.label, this.labelVisible));
    }
}
