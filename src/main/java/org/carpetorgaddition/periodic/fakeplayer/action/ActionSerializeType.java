package org.carpetorgaddition.periodic.fakeplayer.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;

import java.util.Locale;
import java.util.function.Function;

public enum ActionSerializeType {
    /**
     * 停止操作
     */
    STOP(json -> StopAction.INSTANCE),
    /**
     * 物品分拣
     */
    SORTING(json -> {
        String id = json.get(ItemSortingAction.ITEM).getAsString();
        Item item = ItemStackPredicate.stringAsItem(id);
        JsonArray thisVecArray = json.get(ItemSortingAction.THIS_VEC).getAsJsonArray();
        Vec3d thisVec = new Vec3d(
                thisVecArray.get(0).getAsDouble(),
                thisVecArray.get(1).getAsDouble(),
                thisVecArray.get(2).getAsDouble()
        );
        JsonArray otherVecArray = json.get(ItemSortingAction.OTHER_VEC).getAsJsonArray();
        Vec3d otherVec = new Vec3d(
                otherVecArray.get(0).getAsDouble(),
                otherVecArray.get(1).getAsDouble(),
                otherVecArray.get(2).getAsDouble()
        );
        return new ItemSortingAction(null, item, thisVec, otherVec);
    }),
    /**
     * 清空潜影盒
     */
    CLEAN(json -> {
        boolean allItem = json.get(CleanContainerAction.ALL_ITEM).getAsBoolean();
        if (allItem) {
            return new CleanContainerAction(null, null, true);
        }
        Item item = ItemStackPredicate.stringAsItem(json.get(CleanContainerAction.ITEM).getAsString());
        return new CleanContainerAction(null, item, false);
    }),
    /**
     * 填充潜影盒
     */
    FILL(json -> {
        boolean allItem = json.get(FillContainerAction.ALL_ITEM).getAsBoolean();
        boolean dropOther = !json.has(FillContainerAction.DROP_OTHER) || json.get(FillContainerAction.DROP_OTHER).getAsBoolean();
        Item item = allItem ? null : ItemStackPredicate.stringAsItem(json.get(FillContainerAction.ITEM).getAsString());
        return new FillContainerAction(null, item, allItem, dropOther);
    }),
    /**
     * 在工作台合成物品
     */
    CRAFTING_TABLE_CRAFT(json -> {
        ItemStackPredicate[] predicates = new ItemStackPredicate[9];
        for (int i = 0; i < predicates.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            predicates[i] = ItemStackPredicate.load(item);
        }
        return new CraftingTableCraftingAction(null, predicates);
    }),
    /**
     * 在生存模式物品栏合成物品
     */
    INVENTORY_CRAFTING(json -> {
        ItemStackPredicate[] itemStackPredicates = new ItemStackPredicate[4];
        for (int i = 0; i < itemStackPredicates.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            itemStackPredicates[i] = ItemStackPredicate.load(item);
        }
        return new InventoryCraftingAction(null, itemStackPredicates);
    }),
    /**
     * 自动重命名物品
     */
    RENAME(json -> {
        Item item = ItemStackPredicate.stringAsItem(json.get(RenameAction.ITEM).getAsString());
        String newName = json.get(RenameAction.NEW_NAME).getAsString();
        return new RenameAction(null, item, newName);
    }),
    /**
     * 自动使用切石机
     */
    STONECUTTING(json -> {
        Item item = ItemStackPredicate.stringAsItem(json.get(StonecuttingAction.ITEM).getAsString());
        int index = json.get(StonecuttingAction.BUTTON).getAsInt();
        return new StonecuttingAction(null, item, index);
    }),
    /**
     * 自动交易
     */
    TRADE(json -> {
        int index = json.get(TradeAction.INDEX).getAsInt();
        boolean voidTrade = json.get(TradeAction.VOID_TRADE).getAsBoolean();
        return new TradeAction(null, index, voidTrade);
    }),
    /**
     * 自动钓鱼
     */
    FISHING(json -> new FishingAction(null)),
    /**
     * 自动种植
     */
    PLANTING(json -> new FishingAction(null)),
    /**
     * 自动破基岩
     */
    BEDROCK(json -> {
        JsonArray from = json.getAsJsonArray("from");
        JsonArray to = json.getAsJsonArray("to");
        BlockPos minPos = new BlockPos(from.get(0).getAsInt(), from.get(1).getAsInt(), from.get(2).getAsInt());
        BlockPos maxPos = new BlockPos(to.get(0).getAsInt(), to.get(1).getAsInt(), to.get(2).getAsInt());
        return new BedrockAction(null, minPos, maxPos);
    });

    private final String serializedName;
    private final Function<JsonObject, AbstractPlayerAction> deserializer;

    ActionSerializeType(Function<JsonObject, AbstractPlayerAction> deserializer) {
        this.deserializer = deserializer;
        this.serializedName = this.name().toLowerCase(Locale.ROOT);
    }

    public AbstractPlayerAction deserialize(JsonObject json) {
        return this.deserializer.apply(json);
    }

    /**
     * 获取序列化名称
     */
    public String getSerializedName() {
        return this.serializedName;
    }
}
