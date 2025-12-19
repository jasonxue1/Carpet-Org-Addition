package boat.carpetorgaddition.periodic.dialog.builder;

import boat.carpetorgaddition.periodic.dialog.DialogTranslateKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.NoticeDialog;

public final class NoticeDialogBuilder extends DialogBuilder<NoticeDialogBuilder, NoticeDialog> {
    private ActionButton action = ActionButtonBuilder.of(DialogTranslateKeys.ENTRY).build();

    private NoticeDialogBuilder(Component title) {
        super(title);
    }

    public static NoticeDialogBuilder of(Component title) {
        return new NoticeDialogBuilder(title);
    }

    public static NoticeDialogBuilder of(String key, Object... args) {
        return of(TextBuilder.translate(key, args));
    }

    public NoticeDialogBuilder setAction(ActionButton action) {
        this.action = action;
        return this;
    }

    @Override
    protected NoticeDialogBuilder self() {
        return this;
    }

    @Override
    public NoticeDialog build() {
        CommonDialogData data = this.createDialogData();
        return new NoticeDialog(data, this.action);
    }
}
