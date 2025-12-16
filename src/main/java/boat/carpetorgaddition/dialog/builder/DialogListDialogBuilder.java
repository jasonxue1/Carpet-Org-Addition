package boat.carpetorgaddition.dialog.builder;

import boat.carpetorgaddition.dialog.DialogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogListDialog;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DialogListDialogBuilder extends DialogBuilder<DialogListDialogBuilder, DialogListDialog> {
    private final List<Holder<Dialog>> dialogs = new ArrayList<>();
    @Nullable
    private ActionButton exitAction;
    private int columns = 1;
    private int buttonWidth = 150;

    private DialogListDialogBuilder(Component title) {
        super(title);
    }

    public static DialogListDialogBuilder of(Component title) {
        return new DialogListDialogBuilder(title);
    }

    public DialogListDialogBuilder addDialog(Dialog dialog) {
        this.dialogs.add(Holder.direct(dialog));
        return this.self();
    }

    public DialogListDialogBuilder setColumns(int columns) {
        this.columns = columns;
        return this.self();
    }

    public DialogListDialogBuilder setButtonWidth(int buttonWidth) {
        this.buttonWidth = buttonWidth;
        return this.self();
    }

    public DialogListDialogBuilder setExitAction(@Nullable ActionButton exitAction) {
        this.exitAction = exitAction;
        return this;
    }

    public DialogListDialogBuilder setParent(Identifier parent) {
        return this.setExitAction(DialogUtils.createBackButton(parent));
    }

    @Override
    protected DialogListDialogBuilder self() {
        return this;
    }

    @Override
    public DialogListDialog build() {
        CommonDialogData dialogData = this.createDialogData();
        HolderSet<Dialog> set = HolderSet.direct(this.dialogs);
        return new DialogListDialog(dialogData, set, Optional.ofNullable(this.exitAction), this.columns, this.buttonWidth);
    }
}
