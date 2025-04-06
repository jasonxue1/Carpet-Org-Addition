package org.carpetorgaddition.dataupdate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Pair;

import java.util.HashSet;
import java.util.Map;

public class FakePlayerActionDataUpdater implements DataUpdater {
    @Override
    public JsonObject update(JsonObject json, int version) {
        if (version == 0) {
            HashSet<Map.Entry<String, JsonObject>> entries = new HashSet<>();
            // 通常只会循环一次
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                Pair<String, DataUpdater> pair = getDataUpdater(entry.getKey(), version);
                String key = pair.getLeft();
                DataUpdater dataUpdater = pair.getRight();
                // 更新数据
                JsonObject newJson = dataUpdater.update(entry.getValue().getAsJsonObject(), version);
                entries.add(Map.entry(key, newJson));
            }
            JsonObject newJson = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : entries) {
                newJson.add(entry.getKey(), entry.getValue());
            }
            return newJson;
        }
        return json;
    }

    /**
     * @return 左值：键的新名称，右值：数据更新器
     */
    private Pair<String, DataUpdater> getDataUpdater(String key, int version) {
        if (version == 0) {
            return switch (key) {
                case "clean" -> new Pair<>("empty_the_container", new EmptyTheContainerActionDataUpdater());
                case "fill" -> new Pair<>("fill_the_container", new FillTheContainerActionDataUpdater());
                default -> new Pair<>(key, DataUpdater.EMPTY);
            };
        }
        return new Pair<>(key, DataUpdater.EMPTY);
    }
}
