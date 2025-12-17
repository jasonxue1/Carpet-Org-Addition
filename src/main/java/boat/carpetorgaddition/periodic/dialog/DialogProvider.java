package boat.carpetorgaddition.periodic.dialog;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.periodic.dialog.builder.DialogListDialogBuilder;
import boat.carpetorgaddition.periodic.dialog.builder.MultiActionDialogBuilder;
import boat.carpetorgaddition.periodic.dialog.builder.SingleOptionInputBuilder;
import boat.carpetorgaddition.periodic.dialog.builder.TextInputBuilder;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.wheel.TextBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@NullMarked
public class DialogProvider {
    private final Map<Identifier, Supplier<Dialog>> dialogs = new HashMap<>();
    public static final Identifier START = getDialogIdentifier("start");
    public static final Identifier FUNCTION = getDialogIdentifier("function");
    public static final Identifier OPEN_INVENTORY = getDialogIdentifier("open_inventory");
    private final MinecraftServer server;

    public DialogProvider(MinecraftServer server) {
        this.server = server;
        this.init();
    }

    private void init() {
        dialogs.put(START, () -> {
            Component version = new TextBuilder(CarpetOrgAddition.VERSION)
                    .setStringHover(CarpetOrgAddition.BUILD_TIMESTAMP)
                    .build();
            Component component = TextBuilder.create(CarpetOrgAddition.MOD_NAME);
            Component translate = TextBuilder.translate("carpet.dialog.metadata.version", version);
            return DialogListDialogBuilder.of(component)
                    .addDialogBody(translate)
                    .addDialog(getDialog(FUNCTION))
                    .setParent(server, null)
                    .build();
        });
        dialogs.put(FUNCTION, () -> DialogListDialogBuilder.of(TextBuilder.translate("carpet.dialog.function.title"))
                .addDialog(getDialog(OPEN_INVENTORY))
                .setAfterAction(DialogAction.WAIT_FOR_RESPONSE)
                .setParent(this.server, START)
                .build()
        );
        dialogs.put(OPEN_INVENTORY, () -> {
            CommonButtonData data = new CommonButtonData(TextBuilder.translate("carpet.dialog.entry"), CommonButtonData.DEFAULT_WIDTH);
            ActionButton button = new ActionButton(data, Optional.empty());
            return MultiActionDialogBuilder.of(TextBuilder.translate("carpet.dialog.function.open_inventory"))
                    .addAction(button)
                    .setAfterAction(DialogAction.WAIT_FOR_RESPONSE)
                    .addInput(
                            TextInputBuilder.of("uuid")
                                    .setLabel(TextBuilder.translate("carpet.dialog.function.uuid"))
                                    .setMaxLength(36)
                                    .build()
                    )
                    .addInput(
                            SingleOptionInputBuilder.of("type")
                                    .setLabelVisible(false)
                                    .addEntry("inventory", TextBuilder.translate("carpet.generic.inventory"))
                                    .addEntry("ender_chest", TextBuilder.translate("carpet.generic.ender_chest"))
                                    .build()
                    )
                    .setParent(this.server, FUNCTION)
                    .build();
        });
    }

    public Set<Map.Entry<Identifier, Dialog>> entrySet() {
        // 对话框是懒加载的，如果不调用，方法可能返回空集合
        return this.dialogs.entrySet().stream().map(entry -> Map.entry(entry.getKey(), entry.getValue().get())).collect(Collectors.toSet());
    }

    public Dialog getDialog(Identifier key) {
        return Objects.requireNonNull(dialogs.get(key).get(), () -> "Unknown dialog: " + key);
    }

    private static Identifier getDialogIdentifier(String id) {
        return GenericUtils.ofIdentifier("dialog/" + id);
    }
}
