package org.carpetorgaddition.dataupdate;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface DataUpdater {
    String DATA_VERSION = "data_version";
    int VERSION = 1;
    DataUpdater UNCHANGED = (json, version) -> json;

    JsonObject update(JsonObject json, int version);

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
