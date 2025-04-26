package org.carpetorgaddition.dataupdate.player;

import com.google.gson.JsonObject;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.periodic.fakeplayer.action.EmptyTheContainerAction;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;

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
                    predicate = new ItemStackPredicate(ItemStackPredicate.stringAsItem(item));
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
