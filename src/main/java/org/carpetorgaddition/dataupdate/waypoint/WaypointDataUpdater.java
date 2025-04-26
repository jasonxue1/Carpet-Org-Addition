package org.carpetorgaddition.dataupdate.waypoint;

import com.google.gson.JsonObject;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.util.IOUtils;

public class WaypointDataUpdater implements DataUpdater {
    @Override
    public JsonObject update(JsonObject json, int version) {
        if (version == 0) {
            int x = IOUtils.getJsonElement(json, "x", Integer.class).orElseThrow();
            int y = IOUtils.getJsonElement(json, "y", Integer.class).orElseThrow();
            int z = IOUtils.getJsonElement(json, "z", Integer.class).orElseThrow();
            String dimension = json.get("dimension").getAsString();
            String creator = json.get("creator").getAsString();
            String illustrate = IOUtils.getJsonElement(json, "illustrate", "", String.class);
            JsonObject anotherPos = new JsonObject();
            if (IOUtils.jsonHasElement(json, "another_x", "another_y", "another_z")) {
                int another_x = json.get("another_x").getAsInt();
                int another_y = json.get("another_y").getAsInt();
                int another_z = json.get("another_z").getAsInt();
                anotherPos.addProperty("x", another_x);
                anotherPos.addProperty("y", another_y);
                anotherPos.addProperty("z", another_z);
            }
            JsonObject update = new JsonObject();
            JsonObject pos = new JsonObject();
            pos.addProperty("x", x);
            pos.addProperty("y", y);
            pos.addProperty("z", z);
            update.add("pos", pos);
            update.addProperty("dimension", dimension);
            update.addProperty("creator", creator);
            update.addProperty("comment", illustrate);
            update.add("another_pos", anotherPos);
            update.addProperty(DATA_VERSION, VERSION);
            return update;
        }
        return json;
    }
}
