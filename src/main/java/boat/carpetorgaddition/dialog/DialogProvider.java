package boat.carpetorgaddition.dialog;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dialog.builder.DialogListDialogBuilder;
import boat.carpetorgaddition.dialog.builder.MultiActionDialogBuilder;
import boat.carpetorgaddition.dialog.builder.SingleOptionInputBuilder;
import boat.carpetorgaddition.dialog.builder.TextInputBuilder;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.wheel.TextBuilder;
import carpet.CarpetSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.Dialog;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.*;
import java.util.function.Supplier;

@NullMarked
public class DialogProvider {
    @Nullable
    private static String lang;
    private static final Map<Identifier, Dialog> DIALOGS = new HashMap<>();
    public static final Identifier START = getDialogIdentifier("start");
    public static final Identifier FUNCTION = getDialogIdentifier("function");
    public static final Identifier OPEN_INVENTORY = getDialogIdentifier("open_inventory");

    public static Dialog getStartDialog() {
        return getOrCreate(START, () -> {
            Component version = new TextBuilder(CarpetOrgAddition.VERSION)
                    .setStringHover(CarpetOrgAddition.BUILD_TIMESTAMP)
                    .build();
            Component component = TextBuilder.create(CarpetOrgAddition.MOD_NAME);
            Component translate = TextBuilder.translate("carpet.dialog.metadata.version", version);
            return DialogListDialogBuilder.of(component)
                    .addDialogBody(translate)
                    .addDialog(getFunctionDialog())
                    .build();
        });
    }

    public static Dialog getFunctionDialog() {
        return getOrCreate(FUNCTION, () -> DialogListDialogBuilder.of(TextBuilder.translate("carpet.dialog.function.title"))
                .addDialog(getOpenInventoryDialog())
                .setParent(START)
                .build()
        );
    }

    public static Dialog getOpenInventoryDialog() {
        return getOrCreate(OPEN_INVENTORY, () -> {
            CommonButtonData data = new CommonButtonData(TextBuilder.translate("carpet.dialog.entry"), CommonButtonData.DEFAULT_WIDTH);
            ActionButton button = new ActionButton(data, Optional.empty());
            return MultiActionDialogBuilder.of(TextBuilder.translate("carpet.dialog.function.open_inventory"))
                    .addAction(button)
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
                    .setParent(FUNCTION)
                    .build();
        });
    }

    public static Set<Map.Entry<Identifier, Dialog>> entrySet() {
        // 对话框是懒加载的，如果不调用，方法可能返回空集合
        getStartDialog();
        return DIALOGS.entrySet();
    }

    private static Dialog getOrCreate(Identifier key, Supplier<Dialog> supplier) {
        Dialog result = DIALOGS.get(key);
        if (Objects.equals(lang, CarpetSettings.language) || result == null) {
            lang = CarpetSettings.language;
            Dialog dialog = supplier.get();
            DIALOGS.put(key, dialog);
            return dialog;
        }
        return result;
    }

    private static Identifier getDialogIdentifier(String id) {
        return GenericUtils.ofIdentifier("dialog/" + id);
    }
}
