package boat.carpetorgaddition.wheel.nbt;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dataupdate.DataUpdater;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.Optional;

public class NbtWriter {
    private final TagValueOutput output;
    private final MinecraftServer server;
    private static final ProblemReporter REPORTER = new ProblemReporter.ScopedCollector(NbtWriter.class::toString, CarpetOrgAddition.LOGGER);

    public NbtWriter(MinecraftServer server, NbtVersion version) {
        this.output = TagValueOutput.createWithContext(REPORTER, server.registryAccess());
        this.output.store(DataUpdater.DATA_VERSION, NbtVersion.CODEC, version);
        this.server = server;
    }

    public void putIdentifier(String key, Identifier identifier) {
        this.output.store(key, Identifier.CODEC, identifier);
    }

    public NbtReader toReader() {
        return new NbtReader(this.server, this.toNbt());
    }

    public CompoundTag toNbt() {
        return this.output.buildResult();
    }

    public StaticAction toCustomAction(Identifier identifier) {
        return new StaticAction(new ClickEvent.Custom(identifier, Optional.of(this.toNbt())));
    }
}
