package org.carpetorgaddition.rule;

import carpet.api.settings.CarpetRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.wheel.WorldFormat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RuleSelfManager {
    public static final HashMap<String, CarpetRule<?>> RULES = new HashMap<>();
    private final HashMap<String, HashSet<String>> disabledRules = new HashMap<>();
    /**
     * 数据保存的位置
     */
    private final File file;
    /**
     * 规则值是否修改过，如果未修改过，则不重新保存
     */
    private boolean changed = false;

    public RuleSelfManager(MinecraftServer server) {
        WorldFormat worldFormat = new WorldFormat(server, null);
        this.file = worldFormat.file("ruleself", "json");
    }

    /**
     * @return 指定规则是否已启用
     */
    public boolean isEnabled(ServerPlayerEntity player, String rule) {
        if (this.disabledRules.isEmpty()) {
            return false;
        }
        HashSet<String> rules = this.disabledRules.get(FetcherUtils.getPlayerName(player));
        if (rules == null) {
            return false;
        }
        return rules.contains(rule);
    }

    /**
     * 设置规则是否对自己生效
     */
    public void setEnabled(ServerPlayerEntity player, String rule, boolean enabled) {
        String playerName = FetcherUtils.getPlayerName(player);
        HashSet<String> rules = this.disabledRules.get(playerName);
        if (rules == null) {
            if (enabled) {
                HashSet<String> value = new HashSet<>();
                value.add(rule);
                this.disabledRules.put(playerName, value);
                this.changed = true;
            }
            return;
        }
        boolean changed = enabled ? rules.add(rule) : rules.remove(rule);
        if (changed) {
            this.changed = true;
        }
        // 玩家已经关闭了所有仅对自己生效的规则，从集合中删除
        if (rules.isEmpty()) {
            this.disabledRules.remove(playerName);
        }
    }

    public void onServerSave() {
        this.save();
    }

    /**
     * 将功能开关保存为如下的json格式<br>
     * <ul>
     *     <li>根</li>
     * <ul>
     *     <li>{@code data_version}：数据版本</li>
     *     <li>{@code ruleself}：一个json对象，记录每个玩家的功能开关</li>
     *     <ul>
     *         <li>一名玩家，可能不存在</li>
     *           <ul>
     *              <li>一个json数组，记录每一条被禁用的规则，可能不存在</li>
     *          </ul>
     *     </ul>
     * </ul>
     * </ul>
     * 示例：<br>
     * <blockquote>
     *     <pre>
     * {
     *   "data_version": 1,
     *   "ruleself": {
     *     "Steve": [
     *       "blockDropsDirectlyEnterInventory"
     *     ]
     *   }
     * }
     *     </pre>
     * </blockquote>
     */
    public void save() {
        if (this.changed) {
            try {
                JsonObject json = new JsonObject();
                // 添加数据版本属性
                json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
                JsonObject ruleSelfJson = this.createRuleSelfJson();
                json.add("ruleself", ruleSelfJson);
                IOUtils.write(this.file, json);
            } catch (IOException e) {
                IOUtils.loggerError(e);
            }
        }
    }

    private JsonObject createRuleSelfJson() {
        JsonObject json = new JsonObject();
        // json的键为玩家名，值为一个字符串数组
        for (Map.Entry<String, HashSet<String>> entry : this.disabledRules.entrySet()) {
            JsonArray array = new JsonArray();
            HashSet<String> value = entry.getValue();
            for (String rule : value) {
                array.add(rule);
            }
            json.add(entry.getKey(), array);
        }
        return json;
    }

    public void load() {
        if (this.file.isFile()) {
            try {
                JsonObject json = IOUtils.loadJson(this.file);
                JsonObject ruleself = json.get("ruleself").getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : ruleself.entrySet()) {
                    HashSet<String> rules = new HashSet<>();
                    for (JsonElement element : entry.getValue().getAsJsonArray()) {
                        rules.add(element.getAsString());
                    }
                    this.disabledRules.put(entry.getKey(), rules);
                }
            } catch (IOException e) {
                IOUtils.loggerError(e);
            }
        }
    }
}
