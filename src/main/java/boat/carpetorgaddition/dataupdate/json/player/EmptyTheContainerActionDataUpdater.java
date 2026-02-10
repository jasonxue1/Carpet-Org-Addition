package boat.carpetorgaddition.dataupdate.json.player;

import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.periodic.fakeplayer.action.EmptyTheContainerAction;
import boat.carpetorgaddition.util.IdentifierUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import com.google.gson.JsonObject;

/**
 * 清空容器动作数据更新器
 */
public final class EmptyTheContainerActionDataUpdater implements DataUpdater {
    public static final String ALL_ITEM = "allItem";

    @Override
    public JsonObject update(JsonObject oldJson, int version) {
        if (version == 0) {
            if (oldJson.has(ALL_ITEM) && oldJson.get(ALL_ITEM).getAsBoolean()) {
                // 匹配任意物品
                ItemStackPredicate predicate = ItemStackPredicate.WILDCARD;
                JsonObject newJson = new JsonObject();
                newJson.addProperty(EmptyTheContainerAction.ITEM, predicate.toString());
                return newJson;
            } else {
                // 匹配指定物品
                ItemStackPredicate predicate;
                if (oldJson.has(EmptyTheContainerAction.ITEM)) {
                    String item = oldJson.get(EmptyTheContainerAction.ITEM).getAsString();
                    predicate = new ItemStackPredicate(IdentifierUtils.getItem(item));
                } else {
                    predicate = ItemStackPredicate.WILDCARD;
                }
                JsonObject newJson = new JsonObject();
                newJson.addProperty(EmptyTheContainerAction.ITEM, predicate.toString());
                return newJson;
            }
        }
        return oldJson;
    }
}
