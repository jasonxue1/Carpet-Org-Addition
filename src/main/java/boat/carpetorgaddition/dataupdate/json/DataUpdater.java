package boat.carpetorgaddition.dataupdate.json;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface DataUpdater {
    String DATA_VERSION = "data_version";
    @Deprecated(forRemoval = true)
    int VERSION = 3;
    int ZERO = 0;
    DataUpdater UNCHANGED = (json, ignore) -> json;

    JsonObject update(JsonObject oldJson, int version);

    static int getVersion(JsonObject json) {
        if (json.has(DATA_VERSION)) {
            return json.get(DATA_VERSION).getAsInt();
        }
        if (json.has("DataVersion")) {
            return json.get("DataVersion").getAsInt();
        }
        return 0;
    }
}
