package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.wheel.WorldFormat;
import com.google.gson.JsonParseException;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

@NullMarked
public class PlayerSerializationManager {
    public static final String PLAYER_DATA = "player_data";
    public static final String SCRIPT_ACTION = "script_action";
    private final WorldFormat worldFormat;
    private final HashSet<FakePlayerSerializer> serializers = new HashSet<>();
    // TODO 已排序
    private final Map<String, TreeSet<FakePlayerSerializer>> groups = new HashMap<>();
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
        this.serializers.clear();
        this.init();
    }

    private void init() {
        try {
            List<File> list = this.worldFormat.toFileList(WorldFormat.JSON_EXTENSIONS);
            for (File file : list) {
                try {
                    FakePlayerSerializer serializer = new FakePlayerSerializer(file, this);
                    this.serializers.add(serializer);
                    for (String group : serializer.getGroups()) {
                        this.addGroup(group, serializer);
                    }
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
        return this.serializers.stream().sorted(FakePlayerSerializer::compareTo).toList();
    }

    public HashMap<String, HashSet<FakePlayerSerializer>> listGroup(Predicate<FakePlayerSerializer> predicate) {
        HashMap<String, HashSet<FakePlayerSerializer>> map = new HashMap<>();
        List<FakePlayerSerializer> list = this.serializers.stream().filter(predicate).toList();
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

    public Set<FakePlayerSerializer> listGroup(String group) {
        Set<FakePlayerSerializer> set = this.groups.get(group);
        if (set == null) {
            return Set.of();
        }
        return set;
    }

    public void add(FakePlayerSerializer serializer) {
        this.initializeIfNeeded();
        this.serializers.remove(serializer);
        this.serializers.add(serializer);
        serializer.save();
    }

    public Optional<FakePlayerSerializer> get(String name) {
        this.initializeIfNeeded();
        if (name.endsWith(IOUtils.JSON_EXTENSION)) {
            // 如果玩家名称为“xxx.json”，执行到这里可能会抛出异常，但不考虑这种情况
            throw new IllegalArgumentException();
        }
        for (FakePlayerSerializer serializer : this.serializers) {
            if (Objects.equals(serializer.getName(), name)) {
                return Optional.of(serializer);
            }
        }
        return Optional.empty();
    }

    public void addGroup(String group, FakePlayerSerializer serializer) {
        Set<FakePlayerSerializer> set = this.groups.computeIfAbsent(group, _ -> new TreeSet<>());
        set.add(serializer);
    }

    public void removeGroup(String group, FakePlayerSerializer serializer) {
        Set<FakePlayerSerializer> set = this.groups.get(group);
        if (set == null) {
            return;
        }
        set.remove(serializer);
    }

    public int size() {
        return this.serializers.size();
    }

    public void onServerSave() {
        // 如果没有被初始化，则不需要重新保存
        for (FakePlayerSerializer serializer : this.serializers) {
            if (serializer.isChanged()) {
                serializer.save();
            }
        }
    }

    public boolean remove(FakePlayerSerializer serializer) {
        this.initializeIfNeeded();
        boolean remove = this.serializers.remove(serializer);
        return remove && serializer.remove();
    }
}
