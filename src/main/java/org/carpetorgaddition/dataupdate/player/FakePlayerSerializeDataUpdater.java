package org.carpetorgaddition.dataupdate.player;

import com.google.gson.JsonObject;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;

public class FakePlayerSerializeDataUpdater implements DataUpdater {
    @Override
    public JsonObject update(JsonObject json, int version) {
        if (version == 0) {
            // 更新玩家动作数据
            if (json.has(PlayerSerializationManager.SCRIPT_ACTION)) {
                JsonObject scriptJson = json.get(PlayerSerializationManager.SCRIPT_ACTION).getAsJsonObject();
                FakePlayerActionDataUpdater updater = new FakePlayerActionDataUpdater();
                JsonObject newJson = updater.update(scriptJson, version);
                if (newJson != scriptJson) {
                    json.add(PlayerSerializationManager.SCRIPT_ACTION, newJson);
                }
            }
            json.addProperty(DataUpdater.DATA_VERSION, 1);
        }
        return json;
    }
}
