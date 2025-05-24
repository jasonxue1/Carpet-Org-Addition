package org.carpetorgaddition.periodic.fakeplayer;

import net.minecraft.server.MinecraftServer;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.IOUtils;
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

    /**
     * 重新加载玩家数据
     */
    public void reload() {
        this.fakePlayerSerializers.clear();
        this.init();
    }

    private void init() {
        try {
            List<File> list = this.worldFormat.toImmutableFileList(WorldFormat.JSON_EXTENSIONS);
            for (File file : list) {
                try {
                    this.fakePlayerSerializers.add(new FakePlayerSerializer(file));
                } catch (IOException e) {
                    // 译：Failed to load player data successfully
                    CarpetOrgAddition.LOGGER.warn("Failed to load player data successfully", e);
                }
            }
        } catch (RuntimeException e) {
            // 译：加载玩家数据时遇到意外错误
            CarpetOrgAddition.LOGGER.warn("Unexpected error encountered while loading player data", e);
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
        serializer.save();
    }

    public Optional<FakePlayerSerializer> get(String name) {
        if (name.endsWith(IOUtils.JSON_EXTENSION)) {
            // 如果玩家名称为“xxx.json”，执行到这里可能会抛出异常，但不考虑这种情况
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
        for (FakePlayerSerializer serializer : this.fakePlayerSerializers) {
            if (serializer.isChanged()) {
                serializer.save();
            }
        }
    }

    public boolean remove(FakePlayerSerializer serializer) {
        boolean remove = this.fakePlayerSerializers.remove(serializer);
        return remove && serializer.remove();
    }
}
