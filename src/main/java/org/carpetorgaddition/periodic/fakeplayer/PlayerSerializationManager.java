package org.carpetorgaddition.periodic.fakeplayer;

import net.minecraft.server.MinecraftServer;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.wheel.WorldFormat;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PlayerSerializationManager {
    public static final String PLAYER_DATA = "player_data";
    public static final String SCRIPT_ACTION = "script_action";
    private final MinecraftServer server;
    private final WorldFormat worldFormat;
    private final HashSet<FakePlayerSerializer> fakePlayerSerializers = new HashSet<>();
    private boolean initialize = false;

    public PlayerSerializationManager(MinecraftServer server) {
        this.server = server;
        this.worldFormat = new WorldFormat(server, PLAYER_DATA);
    }

    private void initializeIfNeeded() {
        if (this.initialize) {
            return;
        }
        this.init();
        this.initialize = true;
    }

    private void init() {
        List<File> list = this.worldFormat.toImmutableFileList(WorldFormat.JSON_EXTENSIONS);
        for (File file : list) {
            try {
                this.fakePlayerSerializers.add(new FakePlayerSerializer(file));
            } catch (IOException e) {
                // 译：Failed to load player data successfully
                CarpetOrgAddition.LOGGER.warn("Failed to load player data successfully", e);
            }
        }
    }

    /**
     * @return 排序后的玩家序列化列表
     */
    public List<FakePlayerSerializer> list() {
        this.initializeIfNeeded();
        return this.fakePlayerSerializers.stream().sorted(FakePlayerSerializer::compareTo).toList();
    }

    public void add(FakePlayerSerializer serializer) {
        this.fakePlayerSerializers.add(serializer);
    }

    public Optional<FakePlayerSerializer> get(String name) {
        if (name.contains(".")) {
            throw new IllegalArgumentException();
        }
        for (FakePlayerSerializer serializer : this.fakePlayerSerializers) {
            if (Objects.equals(serializer.getFakePlayerName(), name)) {
                return Optional.of(serializer);
            }
        }
        return Optional.empty();
    }

    public void onServerSave() {
        this.fakePlayerSerializers.forEach(FakePlayerSerializer::save);
    }

    public boolean remove(FakePlayerSerializer serializer) {
        boolean remove = this.fakePlayerSerializers.remove(serializer);
        return remove && serializer.remove();
    }
}
