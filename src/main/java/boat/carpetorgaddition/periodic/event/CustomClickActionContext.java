package boat.carpetorgaddition.periodic.event;

import boat.carpetorgaddition.wheel.nbt.NbtReader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CustomClickActionContext {
    private final MinecraftServer server;
    private final ServerPlayer player;
    @Nullable
    private final NbtReader reader;
    public static final ThreadLocal<ServerPlayer> CURRENT_PLAYER = new ThreadLocal<>();

    public CustomClickActionContext(MinecraftServer server, ServerPlayer player, @Nullable NbtReader reader) {
        this.server = server;
        this.player = player;
        this.reader = reader;
    }

    public CustomClickActionContext(MinecraftServer server, ServerPlayer player, Tag tag) {
        NbtReader reader = tag instanceof CompoundTag nbt ? new NbtReader(server, nbt) : null;
        this(server, player, reader);
    }

    public MinecraftServer getServer() {
        return this.server;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public Optional<NbtReader> getReader() {
        return Optional.ofNullable(this.reader);
    }
}
