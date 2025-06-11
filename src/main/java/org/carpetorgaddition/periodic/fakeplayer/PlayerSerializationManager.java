package org.carpetorgaddition.periodic.fakeplayer;

import com.google.gson.JsonParseException;
import net.minecraft.server.MinecraftServer;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.wheel.WorldFormat;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class PlayerSerializationManager {
    public static final String PLAYER_DATA = "player_data";
    public static final String SCRIPT_ACTION = "script_action";
    private final WorldFormat worldFormat;
    private final HashSet<FakePlayerSerializer> fakePlayerSerializers = new HashSet<>();
    private boolean initialize = false;

    public PlayerSerializationManager(MinecraftServer server) {
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
                } catch (IOException | JsonParseException | NullPointerException e) {
                    // 译：未能成功加载玩家数据
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

    public HashMap<String, HashSet<FakePlayerSerializer>> listGroup(Predicate<FakePlayerSerializer> predicate) {
        HashMap<String, HashSet<FakePlayerSerializer>> map = new HashMap<>();
        List<FakePlayerSerializer> list = this.fakePlayerSerializers.stream().filter(predicate).toList();
        for (FakePlayerSerializer serializer : list) {
            for (String group : serializer.getGroups()) {
                HashSet<FakePlayerSerializer> set = map.get(group);
                if (set == null) {
                    HashSet<FakePlayerSerializer> value = new HashSet<>();
                    value.add(serializer);
                    map.put(group, value);
                } else {
                    set.add(serializer);
                }
            }
        }
        return map;
    }

    public void add(FakePlayerSerializer serializer) {
        this.initializeIfNeeded();
        this.fakePlayerSerializers.add(serializer);
        serializer.save();
    }

    public Optional<FakePlayerSerializer> get(String name) {
        this.initializeIfNeeded();
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

    public int size() {
        return this.fakePlayerSerializers.size();
    }

    public void onServerSave() {
        // 如果没有被初始化，则不需要重新保存
        for (FakePlayerSerializer serializer : this.fakePlayerSerializers) {
            if (serializer.isChanged()) {
                serializer.save();
            }
        }
    }

    public boolean remove(FakePlayerSerializer serializer) {
        this.initializeIfNeeded();
        boolean remove = this.fakePlayerSerializers.remove(serializer);
        return remove && serializer.remove();
    }
}
