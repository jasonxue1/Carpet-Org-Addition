package boat.carpetorgaddition.dialog.builder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public abstract class DialogBuilder<C extends DialogBuilder<C, D>, D extends Dialog> {
    private final Component title;
    @Nullable
    private Component externalTitle = null;
    private boolean canCloseWithEscape = true;
    private boolean pause = false;
    private DialogAction afterAction = DialogAction.CLOSE;
    private final List<DialogBody> bodies = new ArrayList<>();
    private final List<Input> inputs = new ArrayList<>();

    protected DialogBuilder(Component title) {
        this.title = title;
    }

    public C setExternalTitle(Component externalTitle) {
        this.externalTitle = externalTitle;
        return this.self();
    }

    public C setCanCloseWithEscape(boolean canCloseWithEscape) {
        this.canCloseWithEscape = canCloseWithEscape;
        return this.self();
    }

    public C setPause(boolean pause) {
        this.pause = pause;
        return this.self();
    }

    public C setAfterAction(DialogAction afterAction) {
        this.afterAction = afterAction;
        return this.self();
    }

    public C addDialogBody(DialogBody body) {
        this.bodies.add(body);
        return this.self();
    }

    public C addDialogBody(Component component) {
        PlainMessage message = new PlainMessage(component, PlainMessage.DEFAULT_WIDTH);
        return this.addDialogBody(message);
    }

    public C addInput(Input input) {
        this.inputs.add(input);
        return this.self();
    }

    protected abstract C self();

    protected CommonDialogData createDialogData() {
        return new CommonDialogData(
                this.title,
                Optional.ofNullable(this.externalTitle),
                this.canCloseWithEscape,
                this.pause,
                this.afterAction,
                this.bodies,
                this.inputs
        );
    }

    public abstract D build();
}
