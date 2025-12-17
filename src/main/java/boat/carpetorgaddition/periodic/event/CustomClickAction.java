package boat.carpetorgaddition.periodic.event;

import boat.carpetorgaddition.wheel.nbt.NbtReader;
import boat.carpetorgaddition.wheel.nbt.NbtVersion;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@NullMarked
public class CustomClickAction {
    private static final Map<Identifier, Consumer<CustomClickActionContext>> ACTIONS = new HashMap<>();
    public static final NbtVersion CURRENT_VERSION = new NbtVersion(1, 0);

    public static void register(Identifier identifier, Consumer<CustomClickActionContext> consumer) {
        Consumer<CustomClickActionContext> value = ACTIONS.put(identifier, consumer);
        if (value != null) {
            throw new IllegalStateException("Duplicate registration: " + identifier);
        }
    }

    public static void accept(Identifier identifier, CustomClickActionContext context) {
        Consumer<CustomClickActionContext> consumer = ACTIONS.get(identifier);
        if (consumer == null) {
            return;
        }
        Optional<NbtReader> optional = context.getReader();
        if (optional.map(reader -> reader.getVersion().compareTo(CURRENT_VERSION) > 0).orElse(false)) {
            // TODO 通知客户端版本不一致
            return;
        }
        consumer.accept(context);
    }
}
