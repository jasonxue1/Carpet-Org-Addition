package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.wheel.WorldFormat;
import com.google.gson.JsonParseException;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
    /**
     * 所有组和组内的玩家<br>
     * {@code null}组表示未分组的玩家
     */
    private final Map<@Nullable String, Set<FakePlayerSerializer>> groups = new HashMap<>();

    public PlayerSerializationManager(MinecraftServer server) {
        this.worldFormat = new WorldFormat(server, PLAYER_DATA);
    }

    /**
     * 重新加载玩家数据
     */
    public void init() {
        this.serializers.clear();
        this.groups.clear();
        this.load();
    }

    private void load() {
        try {
            List<File> list = this.worldFormat.toFileList(WorldFormat.JSON_EXTENSIONS);
            for (File file : list) {
                try {
                    FakePlayerSerializer serializer = new FakePlayerSerializer(file);
                    this.add(serializer);
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
    @Unmodifiable
    public List<FakePlayerSerializer> listAll() {
        return this.serializers.stream().sorted(FakePlayerSerializer::compareTo).toList();
    }

    @Unmodifiable
    public List<FakePlayerSerializer> listGroup(@Nullable String group) {
        Set<FakePlayerSerializer> set = this.groups.get(group);
        return set == null ? List.of() : set.stream().sorted(FakePlayerSerializer::compareTo).toList();
    }

    @Unmodifiable
    public List<FakePlayerSerializer> listUngrouped() {
        return this.listGroup(null);
    }

    public Map<String, List<FakePlayerSerializer>> listGrouped() {
        Map<String, List<FakePlayerSerializer>> map = new HashMap<>();
        for (Map.Entry<@Nullable String, Set<FakePlayerSerializer>> entry : this.groups.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            map.put(key, entry.getValue().stream().sorted(FakePlayerSerializer::compareTo).toList());
        }
        return map;
    }

    public Map<String, Set<FakePlayerSerializer>> listAllGroups() {
        return this.groups;
    }

    /**
     * @return 键表示玩家所在组的名称，值表示组内的所有玩家，键包含一个null值，表示未分组的玩家
     */
    public Map<@Nullable String, Set<FakePlayerSerializer>> listAllGroups(Predicate<FakePlayerSerializer> predicate) {
        HashMap<@Nullable String, Set<FakePlayerSerializer>> map = new HashMap<>();
        List<FakePlayerSerializer> list = this.serializers.stream().filter(predicate).toList();
        for (FakePlayerSerializer serializer : list) {
            Set<String> groups = serializer.getGroups();
            if (groups.isEmpty()) {
                Set<FakePlayerSerializer> set = map.computeIfAbsent(null, _ -> new TreeSet<>());
                set.add(serializer);
            } else {
                for (String group : groups) {
                    Set<FakePlayerSerializer> set = map.computeIfAbsent(group, _ -> new TreeSet<>());
                    set.add(serializer);
                }
            }
        }
        return map;
    }

    public void add(FakePlayerSerializer serializer) {
        this.serializers.add(serializer);
        serializer.addListener(new FakePlayerSerializer.Listener() {
            @Override
            public void onAddGroup(String group) {
                addGroup(group, serializer);
            }

            @Override
            public void onRemoveGroup(String group) {
                removeGroup(group, serializer);
            }
        });
        Set<String> groups = serializer.getGroups();
        if (groups.isEmpty()) {
            Set<FakePlayerSerializer> ungrouped = this.groups.computeIfAbsent(null, _ -> new HashSet<>());
            ungrouped.add(serializer);
        } else {
            for (String group : groups) {
                this.addGroup(group, serializer);
            }
        }
        serializer.save();
    }

    public Optional<FakePlayerSerializer> get(String name) {
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
        if (set.isEmpty()) {
            Set<FakePlayerSerializer> ungrouped = this.groups.get(null);
            if (ungrouped != null) {
                ungrouped.remove(serializer);
            }
        }
        set.add(serializer);
    }

    public void removeGroup(String group, FakePlayerSerializer serializer) {
        Set<FakePlayerSerializer> set = this.groups.get(group);
        if (set == null) {
            return;
        }
        set.remove(serializer);
        if (set.isEmpty()) {
            Set<FakePlayerSerializer> ungrouped = this.groups.computeIfAbsent(null, _ -> new HashSet<>());
            ungrouped.add(serializer);
        }
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
        boolean remove = this.serializers.remove(serializer);
        return remove && serializer.remove();
    }
}
