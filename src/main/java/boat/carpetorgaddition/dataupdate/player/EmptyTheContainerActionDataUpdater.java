package boat.carpetorgaddition.dataupdate.player;

import boat.carpetorgaddition.dataupdate.DataUpdater;
import boat.carpetorgaddition.periodic.fakeplayer.action.EmptyTheContainerAction;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import com.google.gson.JsonObject;

/**
 * 清空容器动作数据更新器
 */
public class EmptyTheContainerActionDataUpdater implements DataUpdater {
    public static final String ALL_ITEM = "allItem";

    @Override
    public JsonObject update(JsonObject json, int version) {
        if (version == 0) {
            if (json.has(ALL_ITEM) && json.get(ALL_ITEM).getAsBoolean()) {
                // 匹配任意物品
                ItemStackPredicate predicate = ItemStackPredicate.WILDCARD;
                JsonObject newJson = new JsonObject();
                newJson.addProperty(EmptyTheContainerAction.ITEM, predicate.toString());
                return newJson;
            } else {
                // 匹配指定物品
                ItemStackPredicate predicate;
                if (json.has(EmptyTheContainerAction.ITEM)) {
                    String item = json.get(EmptyTheContainerAction.ITEM).getAsString();
                    predicate = new ItemStackPredicate(GenericUtils.getItem(item));
                } else {
                    predicate = ItemStackPredicate.WILDCARD;
                }
                JsonObject newJson = new JsonObject();
                newJson.addProperty(EmptyTheContainerAction.ITEM, predicate.toString());
                return newJson;
            }
        }
        return json;
    }
}
