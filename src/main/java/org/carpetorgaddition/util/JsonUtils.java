package org.carpetorgaddition.util;

import com.google.gson.JsonArray;
import net.minecraft.util.math.BlockPos;

public class JsonUtils {
    private JsonUtils() {
    }

    /**
     * 将一个方块坐标转换为json数组
     */
    public static JsonArray toJson(BlockPos blockPos) {
        JsonArray array = new JsonArray();
        array.add(blockPos.getX());
        array.add(blockPos.getY());
        array.add(blockPos.getZ());
        return array;
    }

    /**
     * 将一个json数组转换成一个方块坐标
     */
    public static BlockPos toBlockPos(JsonArray array) {
        int x = array.get(0).getAsInt();
        int y = array.get(1).getAsInt();
        int z = array.get(2).getAsInt();
        return new BlockPos(x, y, z);
    }
}
