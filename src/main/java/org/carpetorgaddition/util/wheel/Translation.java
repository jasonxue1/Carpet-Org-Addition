package org.carpetorgaddition.util.wheel;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.api.settings.SettingsManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionExtension;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Translation {
    private static final String TRANSLATION_PATH = "assets/carpet-org-addition/lang/%s.json";
    private static final Translation TRANSLATION = new Translation();
    private static final Gson GSON = org.carpetorgaddition.util.IOUtils.GSON;
    /**
     * {@code Carpet Org Addition}的所有翻译，键表示语言，值是嵌套的一个Map集合，分别表示翻译的键和值
     */
    private final ConcurrentHashMap<String, Map<String, String>> translations = new ConcurrentHashMap<>();

    private Translation() {
    }

    public static Translation getInstance() {
        return TRANSLATION;
    }

    public Map<String, String> getTranslation() {
        // 每种语言只从文件读取一次
        String lang = CarpetSettings.language;
        return this.translations.computeIfAbsent(lang, this::loadTranslation);
    }

    private Map<String, String> loadTranslation(String lang) {
        try {
            Map<String, String> map = readTranslation(lang);
            if (map.isEmpty()) {
                Map<String, String> enUs = this.translations.get("en_us");
                if (enUs == null) {
                    enUs = this.readTranslation("en_us");
                }
                return enUs;
            }
            return map;
        } catch (IOException e) {
            throw new IllegalStateException("未能成功读取语言文件", e);
        }
    }

    // 从文件读取翻译
    private Map<String, String> readTranslation(String lang) throws IOException {
        ClassLoader classLoader = Translation.class.getClassLoader();
        String path = TRANSLATION_PATH.formatted(lang);
        InputStream input = classLoader.getResourceAsStream(path);
        return readFromInputStream(input);
    }

    private Map<String, String> readFromInputStream(InputStream input) throws IOException {
        if (input == null) {
            return Map.of();
        }
        String json = IOUtils.toString(input, StandardCharsets.UTF_8);
        var typeToken = new TypeToken<Map<String, String>>() {
        };
        Map<String, String> map = GSON.fromJson(json, typeToken);
        return parse(map);
    }

    private Map<String, String> parse(Map<String, String> map) {
        if (CarpetOrgAddition.ALLOW_CUSTOM_SETTINGS_MANAGER) {
            SettingsManager customSettingManager = CarpetOrgAdditionExtension.getCustomSettingManager();
            if (customSettingManager == null) {
                return map;
            }
            String identifier = customSettingManager.identifier();
            if (customSettingManager == CarpetServer.settingsManager || "carpet".equals(identifier)) {
                return map;
            }
            // 如果启用了自定义规则管理器，则更改翻译键以保证Carpet正确读取
            HashMap<String, String> hashMap = new HashMap<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("carpet.rule")) {
                    hashMap.put(identifier + key.substring("carpet".length()), entry.getValue());
                }
            }
            return hashMap;
        }
        return map;
    }

    /**
     * 根据键获取{@code Carpet Org Addition}的翻译，原版和其他模组的翻译不会从这里获取到
     *
     * @param key 翻译键
     * @return 如果翻译来着本模组，返回对应的翻译，如果翻译键本身错误，或着翻译键来自原版或其他模组，返回{@code null}
     */
    @Nullable
    public static String getTranslateValue(String key) {
        Map<String, String> translate = getInstance().getTranslation();
        return translate.get(key);
    }
}
