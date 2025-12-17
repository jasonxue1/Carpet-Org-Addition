package boat.carpetorgaddition.periodic.event;

import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;

import java.util.Optional;
import java.util.function.Consumer;

public class CustomClickEvents {
    public static final Identifier OPEN_DIALOG = register("open_dialog", context -> {
        Optional<NbtReader> optional = context.getReader();
        if (optional.isEmpty()) {
            return;
        }
        NbtReader reader = optional.get();
        Identifier id = reader.getIdentifier("id");
        MinecraftServer server = context.getServer();
        DialogProvider provider = FetcherUtils.getDialogProvider(server);
        Dialog dialog = provider.getDialog(id);
        context.getPlayer().openDialog(Holder.direct(dialog));
    });

    public static Identifier register(String id, Consumer<CustomClickActionContext> consumer) {
        Identifier identifier = GenericUtils.ofIdentifier(id);
        CustomClickAction.register(identifier, consumer);
        return identifier;
    }
}
