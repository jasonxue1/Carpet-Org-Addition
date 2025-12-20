package boat.carpetorgaddition.periodic.dialog.builder;

import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.network.event.CustomClickAction;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.CustomAll;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ActionButtonBuilder {
    private final Component label;
    @Nullable
    private Component tooltip = null;
    private int width = CommonButtonData.DEFAULT_WIDTH;
    @Nullable
    private Action action = null;

    private ActionButtonBuilder(Component label) {
        this.label = label;
    }

    public static ActionButtonBuilder of(Component label) {
        return new ActionButtonBuilder(label);
    }

    @Deprecated
    public static ActionButtonBuilder of(String key, Object... args) {
        return of(TextBuilder.translate(key, args));
    }

    @SuppressWarnings("unused")
    public ActionButtonBuilder setTooltip(@Nullable Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @SuppressWarnings("unused")
    public ActionButtonBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    public ActionButtonBuilder setAction(@Nullable Action action) {
        this.action = action;
        return this;
    }

    public ActionButtonBuilder setCustomClickAction(MinecraftServer server, Identifier id) {
        NbtWriter writer = new NbtWriter(server, CustomClickAction.CURRENT_VERSION);
        writer.putActionSource(ActionSource.DIALOG);
        this.action = new CustomAll(id, Optional.of(writer.toNbt()));
        return this;
    }

    public ActionButton build() {
        CommonButtonData data = new CommonButtonData(this.label, Optional.ofNullable(this.tooltip), this.width);
        return new ActionButton(data, Optional.ofNullable(this.action));
    }
}
