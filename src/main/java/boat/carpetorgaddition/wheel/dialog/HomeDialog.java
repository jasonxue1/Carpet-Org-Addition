package boat.carpetorgaddition.wheel.dialog;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.wheel.TextBuilder;
import boat.carpetorgaddition.wheel.dialog.builder.DialogListDialogBuilder;
import boat.carpetorgaddition.wheel.dialog.builder.MultiActionDialogBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;

import java.util.ArrayList;
import java.util.Optional;

public class HomeDialog {
    private final DialogListDialogBuilder builder;

    public HomeDialog() {
        Component version = new TextBuilder(CarpetOrgAddition.VERSION)
                .setStringHover(CarpetOrgAddition.BUILD_TIMESTAMP)
                .build();
        Component component = TextBuilder.create(CarpetOrgAddition.MOD_NAME);
        Component translate = TextBuilder.translate("carpet.dialog.metadata.version", version);
        this.builder = DialogListDialogBuilder.of(component).addDialogBody(translate);
        this.init();
    }

    private void init() {
        this.builder.addDialog(this.createFunctionAction());
    }

    private DialogListDialog createFunctionAction() {
        return DialogListDialogBuilder.of(TextBuilder.translate("carpet.dialog.function.title"))
                .addDialog(this.createOpenInventoryDialog()).build();
    }

    private MultiActionDialog createOpenInventoryDialog() {
        CommonButtonData data = new CommonButtonData(TextBuilder.translate("carpet.dialog.function.execute"), CommonButtonData.DEFAULT_WIDTH);
        ActionButton button = new ActionButton(data, Optional.empty());
        return MultiActionDialogBuilder.of(TextBuilder.translate("carpet.dialog.function.open_inventory"))
                .addAction(button)
                .addInput(this.getUuidText())
                .addInput(this.getInventoryOption()).build();
    }

    private Input getUuidText() {
        InputControl control = new TextInput(200, TextBuilder.translate("carpet.dialog.function.uuid"), true, "", 36, Optional.empty());
        return new Input("uuid", control);
    }

    private Input getInventoryOption() {
        ArrayList<SingleOptionInput.Entry> list = new ArrayList<>();
        list.add(new SingleOptionInput.Entry("inventory", Optional.of(TextBuilder.translate("carpet.generic.inventory")), true));
        list.add(new SingleOptionInput.Entry("ender_chest", Optional.of(TextBuilder.translate("carpet.generic.ender_chest")), false));
        InputControl control = new SingleOptionInput(200, list, TextBuilder.translate("carpet.generic.type"), false);
        return new Input("type", control);
    }

    public Dialog build() {
        return this.builder.build();
    }
}
