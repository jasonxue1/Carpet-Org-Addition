package boat.carpetorgaddition.periodic.dialog;

import boat.carpetorgaddition.periodic.event.CustomClickAction;
import boat.carpetorgaddition.periodic.event.CustomClickEvents;
import boat.carpetorgaddition.wheel.TextBuilder;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.action.Action;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DialogUtils {
    public static final Component UNDEFINED = TextBuilder.of("carpet.generic.undefined")
            .setColor(ChatFormatting.RED)
            .setBold()
            .build();

    private DialogUtils() {
    }

    public static String toValidDialogKey(String key) {
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
        return key;
    }

    public static ActionButton createBackButton(MinecraftServer server, @Nullable Identifier parent) {
        if (parent == null) {
            CommonButtonData data = new CommonButtonData(TextBuilder.translate(DialogTranslateKeys.CLOSE), CommonButtonData.DEFAULT_WIDTH);
            return new ActionButton(data, Optional.empty());
        } else {
            CommonButtonData data = new CommonButtonData(TextBuilder.translate(DialogTranslateKeys.BACK), CommonButtonData.DEFAULT_WIDTH);
            NbtWriter writer = new NbtWriter(server, CustomClickAction.CURRENT_VERSION);
            writer.putIdentifier("id", parent);
            Action action = writer.toCustomAction(CustomClickEvents.OPEN_DIALOG);
            return new ActionButton(data, Optional.of(action));
        }
    }
}
