package boat.carpetorgaddition.periodic.event;

import boat.carpetorgaddition.command.PlayerCommandExtension;
import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.periodic.dialog.builder.DialogKeys;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;

import java.util.UUID;

public class CustomClickEvents {
    public static final Identifier OPEN_DIALOG = register("open_dialog", context -> {
        NbtReader reader = context.getReader();
        Identifier id = reader.getIdentifier("id");
        MinecraftServer server = context.getServer();
        DialogProvider provider = FetcherUtils.getDialogProvider(server);
        Dialog dialog = provider.getDialog(id);
        context.getPlayer().openDialog(Holder.direct(dialog));
    });
    public static final Identifier OPEN_INVENTORY = register("open_inventory", context -> {
        NbtReader reader = context.getReader();
        UUID uuid = reader
                .getUuidNullable(DialogKeys.UUID)
                .orElseThrow(() -> CommandUtils.createException("carpet.dialog.function.uuid.from_string.fail", reader.getString(DialogKeys.UUID)));
        PlayerInventoryType type = reader.getPlayerInventoryType(DialogKeys.INVENTORY_TYPE);
        PlayerCommandExtension.openPlayerInventory(context.getServer(), uuid, context.getPlayer(), type);
    });

    public static Identifier register(String id, CustomClickAction.CustomClickActionProcessor<CustomClickActionContext> consumer) {
        Identifier identifier = GenericUtils.ofIdentifier(id);
        CustomClickAction.register(identifier, consumer);
        return identifier;
    }
}
