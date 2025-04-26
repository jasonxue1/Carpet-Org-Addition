package org.carpetorgaddition.util.permission;

import carpet.utils.CommandHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.wheel.WorldFormat;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PermissionManager {
    private static final HashMap<String, CommandPermission> PERMISSIONS = new HashMap<>();
    private static final String PERMISSION_JSON = "permission.json";

    public static CommandPermission register(String node, PermissionLevel level) {
        CommandPermission permission = new CommandPermission(level);
        PERMISSIONS.put(node, permission);
        return permission;
    }

    public static CommandPermission registerHiddenCommand(String node, PermissionLevel level) {
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            return register(node, level);
        }
        return new CommandPermission(level);
    }

    /**
     * 列出所有权限节点
     */
    public static List<String> listNode() {
        return PERMISSIONS.keySet().stream().toList();
    }

    /**
     * 获取指定权限节点
     */
    @Nullable
    public static CommandPermission getPermission(String node) {
        return PERMISSIONS.get(node);
    }

    public static void reset() {
        PERMISSIONS.clear();
    }

    /**
     * 将命令权限保存到文件
     */
    public static void save(MinecraftServer server) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
        JsonObject permission = new JsonObject();
        PERMISSIONS.forEach((node, perm) -> permission.addProperty(node, perm.getLevel().asString()));
        json.add("permission", permission);
        WorldFormat worldFormat = new WorldFormat(server, null);
        File file = worldFormat.file(PERMISSION_JSON);
        IOUtils.saveJson(file, json);
    }

    /**
     * 从文件读取命令权限
     */
    public static void load(MinecraftServer server) {
        WorldFormat worldFormat = new WorldFormat(server, null);
        File file = worldFormat.file(PERMISSION_JSON);
        if (file.isFile()) {
            JsonObject json;
            try {
                json = IOUtils.loadJson(file);
            } catch (IOException e) {
                IOUtils.loggerError(e);
                return;
            }
            Set<Map.Entry<String, JsonElement>> entries = json.get("permission").getAsJsonObject().entrySet();
            for (Map.Entry<String, JsonElement> entry : entries) {
                CommandPermission permission = PERMISSIONS.get(entry.getKey());
                PermissionLevel level;
                try {
                    level = PermissionLevel.fromString(entry.getValue().getAsString());
                } catch (IllegalArgumentException e) {
                    CarpetOrgAddition.LOGGER.warn("Unable to parse permissions for permission node {}", entry.getKey(), e);
                    continue;
                }
                permission.setLevel(level);
            }
            CommandHelper.notifyPlayersCommandsChanged(server);
        }
    }
}
