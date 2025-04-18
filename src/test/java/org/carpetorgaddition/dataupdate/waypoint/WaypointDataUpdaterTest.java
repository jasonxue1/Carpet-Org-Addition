package org.carpetorgaddition.dataupdate.waypoint;

import com.google.gson.JsonObject;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.util.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WaypointDataUpdaterTest {
    @Test
    public void testDataUpdate() {
        String oldWaypoint = """
                {
                  "x": 1,
                  "y": 2,
                  "z": 3,
                  "dimension": "minecraft:overworld",
                  "creator": "Steve",
                  "illustrate": "测试",
                  "another_x": 8,
                  "another_y": 16,
                  "another_z": 24
                }
                """;
        JsonObject json = IOUtils.GSON.fromJson(oldWaypoint, JsonObject.class);
        WaypointDataUpdater dataUpdater = new WaypointDataUpdater();
        JsonObject update = dataUpdater.update(json, DataUpdater.getVersion(json));
        // 路径点坐标已被替换为数组
        Assertions.assertNull(update.get("x"));
        Assertions.assertNull(update.get("y"));
        Assertions.assertNull(update.get("z"));
        Assertions.assertNotNull(update.get("pos"));
        // 维度和创建者保持不变
        Assertions.assertNotNull(update.get("dimension"));
        Assertions.assertNotNull(update.get("creator"));
        // illustrate已被重命名为comment
        Assertions.assertNull(update.get("illustrate"));
        Assertions.assertNotNull(update.get("comment"));
        // 路径点的另一个坐标已被替换为数组
        Assertions.assertNull(update.get("another_x"));
        Assertions.assertNull(update.get("another_y"));
        Assertions.assertNull(update.get("another_z"));
        String newWaypoint = """
                {
                  "pos": {
                      "x": 1,
                      "y": 2,
                      "z": 3
                  },
                  "dimension": "minecraft:overworld",
                  "creator": "Steve",
                  "comment": "测试",
                  "another_pos": {
                      "x": 8,
                      "y": 16,
                      "z": 24
                  },
                  "data_version": 1
                }
                """;
        JsonObject newJson = IOUtils.GSON.fromJson(newWaypoint, JsonObject.class);
        Assertions.assertEquals(update, newJson);
    }
}
