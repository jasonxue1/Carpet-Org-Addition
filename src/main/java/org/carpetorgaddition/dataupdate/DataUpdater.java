package org.carpetorgaddition.dataupdate;

import com.google.gson.JsonObject;
import org.carpetorgaddition.periodic.fakeplayer.action.ActionSerializeType;

@FunctionalInterface
public interface DataUpdater {
    String DATA_VERSION = "DataVersion";
    int VERSION = 1;

    JsonObject update(JsonObject json, int version);

    static int getVersion(JsonObject json) {
        if (json.has(DATA_VERSION)) {
            return json.get(DATA_VERSION).getAsInt();
        }
        return 0;
    }

    static DataUpdater dataUpdaterFactory(ActionSerializeType type) {
        return switch (type) {
            case EMPTY_THE_CONTAINER -> new EmptyTheContainerActionDataUpdater();
            default -> (json, version) -> json;
        };
    }
}
