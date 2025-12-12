package boat.carpetorgaddition.dataupdate.player;

import boat.carpetorgaddition.dataupdate.DataUpdater;
import boat.carpetorgaddition.periodic.fakeplayer.action.FillTheContainerAction;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import com.google.gson.JsonObject;

public class FillTheContainerActionDataUpdater implements DataUpdater {
    private static final String ALL_ITEM = "allItem";

    @Override
    public JsonObject update(JsonObject json, int version) {
        if (version == 0) {
            String key = FillTheContainerAction.DROP_OTHER;
            // dropOther默认为true
            boolean dropOther = !json.has(key) || json.get(key).getAsBoolean();
            ItemStackPredicate predicate;
            if (json.has(ALL_ITEM) && json.get(ALL_ITEM).getAsBoolean()) {
                // 匹配任意物品
                predicate = ItemStackPredicate.WILDCARD;
            } else if (json.has(FillTheContainerAction.ITEM)) {
                // 匹配指定物品
                String itemId = json.get(FillTheContainerAction.ITEM).getAsString();
                predicate = new ItemStackPredicate(GenericUtils.getItem(itemId));
            } else {
                predicate = ItemStackPredicate.WILDCARD;
            }
            JsonObject newJson = new JsonObject();
            newJson.addProperty(FillTheContainerAction.ITEM, predicate.toString());
            newJson.addProperty(FillTheContainerAction.DROP_OTHER, dropOther);
            return newJson;
        }
        return json;
    }
}
