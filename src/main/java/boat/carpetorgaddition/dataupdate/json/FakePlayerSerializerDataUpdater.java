package boat.carpetorgaddition.dataupdate.json;

import boat.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import com.google.gson.JsonObject;

public final class FakePlayerSerializerDataUpdater implements DataUpdater {
    private static final FakePlayerSerializerDataUpdater INSTANCE = new FakePlayerSerializerDataUpdater();

    private FakePlayerSerializerDataUpdater() {
    }

    public static FakePlayerSerializerDataUpdater getInstance() {
        return INSTANCE;
    }

    @Override
    public JsonObject update(JsonObject oldJson, int version) {
        return switch (version) {
            case 0, 1, 2 -> {
                // 更新玩家动作数据
                if (oldJson.has(PlayerSerializationManager.SCRIPT_ACTION)) {
                    JsonObject scriptJson = oldJson.get(PlayerSerializationManager.SCRIPT_ACTION).getAsJsonObject();
                    FakePlayerActionDataUpdater updater = FakePlayerActionDataUpdater.getInstance();
                    JsonObject newJson = updater.update(scriptJson, version);
                    if (newJson != scriptJson) {
                        oldJson.add(PlayerSerializationManager.SCRIPT_ACTION, newJson);
                    }
                }
                oldJson.addProperty(DataUpdater.DATA_VERSION, 1);
                yield this.update(oldJson, 1);
            }
            default -> oldJson;
        };
    }
}
