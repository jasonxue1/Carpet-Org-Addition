package org.carpetorgaddition.dataupdate;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface DataUpdater {
    // TODO 更改为小写下划线命名
    String DATA_VERSION = "DataVersion";
    int VERSION = 1;
    DataUpdater UNCHANGED = (json, version) -> json;

    JsonObject update(JsonObject json, int version);

    static int getVersion(JsonObject json) {
        if (json.has(DATA_VERSION)) {
            return json.get(DATA_VERSION).getAsInt();
        }
        return 0;
    }
}
