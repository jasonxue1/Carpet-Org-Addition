package boat.carpetorgaddition.wheel.nbt;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dataupdate.DataUpdater;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtFormatException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

public class NbtReader {
    private static final ProblemReporter REPORTER = new ProblemReporter.ScopedCollector(NbtReader.class::toString, CarpetOrgAddition.LOGGER);
    private final ValueInput input;

    public NbtReader(MinecraftServer server, CompoundTag tag) {
        this.input = TagValueInput.create(REPORTER, server.registryAccess(), tag);
    }

    public Identifier getIdentifier(String key) {
        return this.input.read(key, Identifier.CODEC).orElseThrow();
    }

    public NbtVersion getVersion() {
        return this.input.read(DataUpdater.DATA_VERSION, NbtVersion.CODEC).orElseThrow(() -> new NbtFormatException("Missing version number information"));
    }
}
