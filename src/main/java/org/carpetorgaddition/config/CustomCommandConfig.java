package org.carpetorgaddition.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomCommandConfig {
    private final File file = IOUtils.createConfigFile("custom_command_name.json", true);
    private final HashMap<String, Set<String>> commands = new HashMap<>();
    /**
     * 当前命令的配置文件是否已经失效
     */
    private boolean expired = false;
    private static final CustomCommandConfig INSTANCE = new CustomCommandConfig();

    public static CustomCommandConfig getInstance() {
        return INSTANCE;
    }

    private CustomCommandConfig() {
        if (this.file.length() == 0) {
            this.init();
        }
        this.load();
    }

    /**
     * 初始化命令配置文件
     */
    private void init() {
        try {
            this.createConfigFile();
        } catch (RuntimeException e) {
            // 译：创建自定义命令名称配置文件时遇到意外错误。
            CarpetOrgAddition.LOGGER.warn("An unexpected error occurred while creating the custom command names configuration file: ", e);
        }
    }

    /**
     * 如果配置文件失效，则重新生成配置文件，方法可能在渲染线程和Worker-Main线程同时调用
     */
    public synchronized void refreshIfExpired() {
        if (this.expired) {
            this.init();
        }
    }

    /**
     * 创建配置文件
     */
    private void createConfigFile() {
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
        JsonObject command = new JsonObject();
        for (Map.Entry<String, Set<String>> entry : this.commands.entrySet()) {
            String key = entry.getKey();
            Set<String> value = entry.getValue();
            if (value.isEmpty()) {
                // 如果值为空，将键同时做为键和值
                command.addProperty(key, key);
            } else if (value.size() == 1) {
                // 如果值只有应该元素，则值作为字符串
                command.addProperty(key, value.iterator().next());
            } else {
                // 如果值有多个元素，则封装为Json数组
                JsonArray array = new JsonArray();
                for (String str : value) {
                    array.add(str);
                }
                command.add(key, array);
            }
        }
        json.add("commands", command);
        try {
            IOUtils.saveJson(this.file, json);
            this.expired = false;
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
    }

    /**
     * 加载配置文件，方法只会在构造方法中执行一次，因此是线程安全的
     */
    public void load() {
        try {
            JsonObject json = IOUtils.loadJson(this.file);
            JsonObject command = json.get("commands").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : command.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                Set<String> set = switch (value) {
                    case JsonPrimitive primitive when primitive.isString() -> Set.of(value.getAsString());
                    case JsonArray array -> array.asList().stream()
                            .filter(JsonElement::isJsonPrimitive)
                            .map(JsonElement::getAsString)
                            .collect(Collectors.toSet());
                    case null, default -> Set.of(key);
                };
                this.commands.put(key, set);
            }
            return;
        } catch (IOException e) {
            IOUtils.loggerError(e);
        } catch (RuntimeException e) {
            // 译：自定义命令名称配置文件已损坏，尝试重新生成。
            CarpetOrgAddition.LOGGER.warn("The custom command name configuration file is damaged. Attempting to regenerate it: ", e);
            // 重新生成前备份文件
            IOUtils.backup(this.file);
        }
        this.expired = true;
    }

    /**
     * 获取命令的自定义名称，如果不存在，返回参数本身做为默认值
     *
     * @implNote 游戏会在注册命令时从两个线程多次调用本方法，此后不会再调用，<br>
     * 并且每次调用都只会传入不同的参数
     */
    public String[] getCommand(String command) {
        Set<String> set;
        synchronized (this) {
            set = this.commands.get(command);
            if (set == null) {
                set = Set.of(command);
                this.commands.put(command, set);
                this.expired = true;
            }
        }
        return set.toArray(String[]::new);
    }
}
