package boat.carpetorgaddition.dataupdate.json.player;

import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Tuple;

import java.util.HashSet;
import java.util.Map;

public final class FakePlayerActionDataUpdater implements DataUpdater {
    @Override
    public JsonObject update(JsonObject oldJson, int version) {
        if (version == 0) {
            HashSet<Map.Entry<String, JsonObject>> entries = new HashSet<>();
            // 通常只会循环一次
            for (Map.Entry<String, JsonElement> entry : oldJson.entrySet()) {
                Tuple<String, DataUpdater> pair = getDataUpdater(entry.getKey(), version);
                String key = pair.getA();
                DataUpdater dataUpdater = pair.getB();
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
        return oldJson;
    }

    /**
     * @return 左值：键的新名称，右值：数据更新器
     */
    private Tuple<String, DataUpdater> getDataUpdater(String key, int version) {
        if (version == 0) {
            return switch (key) {
                case "clean" -> new Tuple<>("empty_the_container", new EmptyTheContainerActionDataUpdater());
                case "fill" -> new Tuple<>("fill_the_container", new FillTheContainerActionDataUpdater());
                case "inventory_crafting" -> new Tuple<>("inventory_craft", DataUpdater.UNCHANGED);
                case "sorting" -> new Tuple<>("categorize", DataUpdater.UNCHANGED);
                case "planting" -> new Tuple<>("plant", DataUpdater.UNCHANGED);
                default -> new Tuple<>(key, DataUpdater.UNCHANGED);
            };
        }
        return new Tuple<>(key, DataUpdater.UNCHANGED);
    }
}
