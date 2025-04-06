package org.carpetorgaddition.dataupdate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Map;

public class FakePlayerActionDataUpdater implements DataUpdater {
    @Override
    public JsonObject update(JsonObject json, int version) {
        if (version == 0) {
            HashSet<Map.Entry<String, JsonObject>> entries = new HashSet<>();
            // 通常只会循环一次
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                Map.Entry<String, JsonObject> newEntry = switch (entry.getKey()) {
                    case "clean" -> {
                        EmptyTheContainerActionDataUpdater updater = new EmptyTheContainerActionDataUpdater();
                        JsonObject update = updater.update(entry.getValue().getAsJsonObject(), version);
                        yield Map.entry("empty_the_container", update);
                    }
                    // TODO
                    case "fill" -> {
                        FillTheContainerActionDataUpdater dataUpdater = new FillTheContainerActionDataUpdater();
                        JsonObject update = dataUpdater.update(entry.getValue().getAsJsonObject(), version);
                        yield Map.entry("fill_the_container", update);
                    }
                    default -> Map.entry(entry.getKey(), entry.getValue().getAsJsonObject());
                };
                entries.add(newEntry);
            }
            JsonObject newJson = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : entries) {
                newJson.add(entry.getKey(), entry.getValue());
            }
            return newJson;
        }
        return json;
    }
}
