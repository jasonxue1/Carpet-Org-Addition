package boat.carpetorgaddition.dataupdate.json.player;

import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Map;

public final class FakePlayerActionDataUpdater implements DataUpdater {
    private static final FakePlayerActionDataUpdater INSTANCE = new FakePlayerActionDataUpdater();

    private FakePlayerActionDataUpdater() {
    }

    public static FakePlayerActionDataUpdater getInstance() {
        return INSTANCE;
    }

    @Override
    public JsonObject update(JsonObject oldJson, int version) {
        if (version == 0) {
            HashSet<Map.Entry<String, JsonObject>> entries = new HashSet<>();
            // 通常只会循环一次，因为一名玩家只能同时有一个动作
            for (Map.Entry<String, JsonElement> oldActionEntry : oldJson.entrySet()) {
                Map.Entry<String, DataUpdater> actionUpdaterEntry = getDataUpdater(oldActionEntry.getKey(), version);
                String key = actionUpdaterEntry.getKey();
                DataUpdater dataUpdater = actionUpdaterEntry.getValue();
                // 更新数据
                JsonObject newJson = dataUpdater.update(oldActionEntry.getValue().getAsJsonObject(), version);
                entries.add(Map.entry(key, newJson));
            }
            JsonObject newJson = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : entries) {
                newJson.add(entry.getKey(), entry.getValue());
            }
            return newJson;
        }
        return oldJson;
    }

    /**
     * @return 左值：键的新名称，右值：数据更新器
     */
    private Map.Entry<String, DataUpdater> getDataUpdater(String key, int version) {
        if (version == 0) {
            return switch (key) {
                case "clean" -> Map.entry("empty_the_container", EmptyTheContainerActionDataUpdater.getInstance());
                case "fill" -> Map.entry("fill_the_container", FillTheContainerActionDataUpdater.getInstance());
                case "inventory_crafting" -> Map.entry("inventory_craft", DataUpdater.UNCHANGED);
                case "sorting" -> Map.entry("categorize", DataUpdater.UNCHANGED);
                case "planting" -> Map.entry("plant", DataUpdater.UNCHANGED);
                default -> Map.entry(key, DataUpdater.UNCHANGED);
            };
        }
        return Map.entry(key, DataUpdater.UNCHANGED);
    }
}
