package boat.carpetorgaddition.dataupdate.json.player;

import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.periodic.fakeplayer.action.FillTheContainerAction;
import boat.carpetorgaddition.util.IdentifierUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import com.google.gson.JsonObject;

public final class FillTheContainerActionDataUpdater implements DataUpdater {
    private static final FillTheContainerActionDataUpdater INSTANCE = new FillTheContainerActionDataUpdater();

    private FillTheContainerActionDataUpdater() {
    }

    public static FillTheContainerActionDataUpdater getInstance() {
        return INSTANCE;
    }

    @Override
    public JsonObject update(JsonObject oldJson, int version) {
        if (version == 0) {
            String key = FillTheContainerAction.DROP_OTHER;
            // dropOther默认为true
            boolean dropOther = !oldJson.has(key) || oldJson.get(key).getAsBoolean();
            ItemStackPredicate predicate;
            if (oldJson.has("allItem") && oldJson.get("allItem").getAsBoolean()) {
                // 匹配任意物品
                predicate = ItemStackPredicate.WILDCARD;
            } else if (oldJson.has(FillTheContainerAction.ITEM)) {
                // 匹配指定物品
                String itemId = oldJson.get(FillTheContainerAction.ITEM).getAsString();
                predicate = new ItemStackPredicate(IdentifierUtils.getItem(itemId));
            } else {
                predicate = ItemStackPredicate.WILDCARD;
            }
            JsonObject newJson = new JsonObject();
            newJson.addProperty(FillTheContainerAction.ITEM, predicate.toString());
            newJson.addProperty(FillTheContainerAction.DROP_OTHER, dropOther);
            return newJson;
        }
        return oldJson;
    }
}
