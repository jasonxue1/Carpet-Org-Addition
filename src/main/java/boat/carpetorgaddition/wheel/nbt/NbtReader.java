package boat.carpetorgaddition.wheel.nbt;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dataupdate.DataUpdater;
import boat.carpetorgaddition.network.event.ActionSource;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class NbtReader {
    private static final ProblemReporter REPORTER = new ProblemReporter.ScopedCollector(NbtReader.class::toString, CarpetOrgAddition.LOGGER);
    private final ValueInput input;

    public NbtReader(MinecraftServer server, CompoundTag tag) {
        this.input = TagValueInput.create(REPORTER, server.registryAccess(), tag);
    }

    public String getString(String key) {
        return this.input.getString(key).orElseThrow();
    }

    public int getInt(String key) {
        return this.input.getInt(key).orElseThrow();
    }

    public Identifier getIdentifier(String key) {
        return getIdentifierNullable(key).orElseThrow();
    }

    private Optional<Identifier> getIdentifierNullable(String key) {
        // TODO 检查UUID是否有效
        return this.input.read(key, Identifier.CODEC);
    }

    public UUID getUuid(String key) {
        return this.getUuidNullable(key).orElseThrow();
    }

    public Optional<UUID> getUuidNullable(String key) {
        return this.input.read(key, UUIDUtil.STRING_CODEC);
    }

    public PlayerInventoryType getPlayerInventoryType(String key) {
        return this.input.read(key, PlayerInventoryType.CODEC).orElseThrow();
    }

    public ActionSource getActionSource() {
        return this.getActionSourceNullable().orElse(ActionSource.UNKNOWN);
    }

    public Optional<ActionSource> getActionSourceNullable() {
        return this.input.read("action_source", ActionSource.CODEC);
    }

    public NbtVersion getVersion() {
        return this.input.read(DataUpdater.DATA_VERSION, NbtVersion.CODEC).orElseThrow(() -> new NbtFormatException("Missing version number information"));
    }
}
