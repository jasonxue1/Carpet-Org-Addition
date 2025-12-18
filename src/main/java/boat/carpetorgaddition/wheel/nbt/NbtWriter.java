package boat.carpetorgaddition.wheel.nbt;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dataupdate.DataUpdater;
import boat.carpetorgaddition.periodic.event.ActionSource;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public class NbtWriter {
    private final TagValueOutput output;
    private static final ProblemReporter REPORTER = new ProblemReporter.ScopedCollector(NbtWriter.class::toString, CarpetOrgAddition.LOGGER);

    public NbtWriter(MinecraftServer server, NbtVersion version) {
        this.output = TagValueOutput.createWithContext(REPORTER, server.registryAccess());
        this.output.store(DataUpdater.DATA_VERSION, NbtVersion.CODEC, version);
    }

    public void putIdentifier(String key, Identifier identifier) {
        this.output.store(key, Identifier.CODEC, identifier);
    }

    public void putUuid(String key, UUID uuid) {
        this.output.store(key, UUIDUtil.STRING_CODEC, uuid);
    }

    public void putPlayerInventoryType(String key, PlayerInventoryType type) {
        this.output.store(key, PlayerInventoryType.CODEC, type);
    }

    public void putActionSource(ActionSource source) {
        this.output.store("action_source", ActionSource.CODEC, source);
    }

    public CompoundTag toNbt() {
        return this.output.buildResult();
    }

    public StaticAction toCustomAction(Identifier identifier, ActionSource source) {
        this.putActionSource(source);
        return new StaticAction(new ClickEvent.Custom(identifier, Optional.of(this.toNbt())));
    }
}
