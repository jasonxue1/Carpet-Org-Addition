package boat.carpetorgaddition.network.event;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import boat.carpetorgaddition.wheel.nbt.NbtVersion;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;

@NullMarked
public class CustomClickAction {
    private static final Map<Identifier, CustomClickActionProcessor> ACTIONS = new HashMap<>();
    public static final NbtVersion CURRENT_VERSION = new NbtVersion(1, 0);

    public static void register(Identifier identifier, CustomClickActionProcessor consumer) {
        CustomClickActionProcessor value = ACTIONS.put(identifier, consumer);
        if (value != null) {
            throw new IllegalStateException("Duplicate registration: " + identifier);
        }
    }

    public static void accept(Identifier identifier, CustomClickActionContext context) {
        CustomClickActionProcessor processor = ACTIONS.get(identifier);
        if (processor == null) {
            return;
        }
        try {
            NbtReader reader = context.getReader();
            if (reader.getVersion().compareTo(CURRENT_VERSION) > 0) {
                // TODO 通知客户端版本不一致
                return;
            }
        } catch (NullPointerException e) {
            CarpetOrgAddition.LOGGER.warn("{} contains no nbt data", identifier, e);
        }
        try {
            processor.accept(context);
        } catch (CommandSyntaxException e) {
            switch (context.getActionSource()) {
                case CHAT -> MessageUtils.sendVanillaErrorMessage(context.getSource(), e);
                case DIALOG -> {
                    DialogProvider provider = FetcherUtils.getDialogProvider(context.getServer());
                    Dialog dialog = provider.createErrorNoticeDialog(e);
                    PlayerUtils.openDialog(context.getPlayer(), dialog);
                }
                case SIGN -> MessageUtils.sendErrorMessageToHud(context.getSource(), e);
                case UNKNOWN -> {
                    if (CarpetOrgAddition.isDebugDevelopment()) {
                        CarpetOrgAddition.LOGGER.warn("Unexpected problem while executing custom action from unknown source", e);
                    }
                }
            }
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("Unexpected error while processing custom click action for identifier [{}]: ", identifier, e);
        }
    }

    @FunctionalInterface
    public interface CustomClickActionProcessor {
        void accept(CustomClickActionContext context) throws CommandSyntaxException;
    }
}
