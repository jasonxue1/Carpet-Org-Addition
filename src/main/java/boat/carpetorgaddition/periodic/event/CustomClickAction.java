package boat.carpetorgaddition.periodic.event;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import boat.carpetorgaddition.wheel.nbt.NbtVersion;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@NullMarked
public class CustomClickAction {
    private static final Map<Identifier, CustomClickActionProcessor<CustomClickActionContext>> ACTIONS = new HashMap<>();
    public static final NbtVersion CURRENT_VERSION = new NbtVersion(1, 0);

    public static void register(Identifier identifier, CustomClickActionProcessor<CustomClickActionContext> consumer) {
        CustomClickActionProcessor<CustomClickActionContext> value = ACTIONS.put(identifier, consumer);
        if (value != null) {
            throw new IllegalStateException("Duplicate registration: " + identifier);
        }
    }

    public static void accept(Identifier identifier, CustomClickActionContext context) {
        CustomClickActionProcessor<CustomClickActionContext> processor = ACTIONS.get(identifier);
        if (processor == null) {
            return;
        }
        Optional<NbtReader> optional = context.getReaderNullable();
        if (optional.map(reader -> reader.getVersion().compareTo(CURRENT_VERSION) > 0).orElse(false)) {
            // TODO 通知客户端版本不一致
            return;
        }
        try {
            processor.accept(context);
        } catch (CommandSyntaxException e) {
            DialogProvider provider = FetcherUtils.getDialogProvider(context.getServer());
            Dialog dialog = provider.createErrorNoticeDialog(e);
            context.getPlayer().openDialog(Holder.direct(dialog));
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("Unexpected error while processing custom click action for identifier [{}]: ", identifier, e);
        }
    }

    @FunctionalInterface
    public interface CustomClickActionProcessor<T> {
        void accept(T value) throws CommandSyntaxException;
    }
}
