package org.carpetorgaddition.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.wheel.LazyValue;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class GlobalConfigs {
    /**
     * 配置文件的路径
     */
    private static final LazyValue<File> CONFIG_FILE = new LazyValue<>(
            IOUtils.CONFIGURE_DIRECTORY.resolve(CarpetOrgAddition.MOD_ID + ".json").toFile(),
            GlobalConfigs::init
    );
    /**
     * 是否启用隐藏功能
     */
    private static final String ENABLE_HIDDEN_FUNCTION = "enableHiddenFunction";
    private static final HashSet<AbstractConfig<?>> CONFIGURATIONS = new HashSet<>();
    public static final CustomCommandConfig CUSTOM_COMMAND_NAMES = register(new CustomCommandConfig());
    private static boolean updateRequired = false;

    private GlobalConfigs() {
    }

    /**
     * 初始化配置文件
     */
    private static void init(File file) {
        if (file.isFile()) {
            return;
        }
        CarpetOrgAddition.LOGGER.info("Initializing configuration file");
        try {
            JsonObject json = new JsonObject();
            json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
            IOUtils.write(file, json);
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
    }

    /**
     * @return 是否启用了隐藏功能
     */
    public static boolean isEnableHiddenFunction() {
        try {
            JsonObject json = IOUtils.loadJson(CONFIG_FILE.get());
            return json.has(ENABLE_HIDDEN_FUNCTION) && json.get(ENABLE_HIDDEN_FUNCTION).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    public static void save() {
        try {
            File file = CONFIG_FILE.get();
            JsonObject json = IOUtils.loadJson(file);
            for (AbstractConfig<?> configuration : CONFIGURATIONS) {
                json.add(configuration.getKey(), configuration.getJsonValue());
            }
            IOUtils.write(file, json);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("An unexpected error occurred while saving the global configuration file for {}", CarpetOrgAddition.MOD_NAME, e);
        }
        updateRequired = false;
    }

    /**
     * 标记为需要更新
     */
    public static void markUpdateRequired() {
        updateRequired = true;
    }

    @SuppressWarnings("unchecked")
    public static <E extends JsonElement, C extends AbstractConfig<E>> void load(C config) {
        JsonObject json;
        File file = CONFIG_FILE.get();
        try {
            json = IOUtils.loadJson(file);
        } catch (IOException e) {
            IOUtils.loggerError(e);
            return;
        }
        JsonElement element = json.get(config.getKey());
        try {
            config.load((E) element);
        } catch (RuntimeException e) {
            IOUtils.backupFile(file);
            json.add(config.getKey(), config.getJsonValue());
            markUpdateRequired();
            CarpetOrgAddition.LOGGER.warn("Global config partially corrupted - resetting damaged section", e);
        }
    }

    private static <E extends JsonElement, C extends AbstractConfig<E>> C register(C config) {
        CONFIGURATIONS.add(config);
        load(config);
        if (updateRequired) {
            save();
        }
        return config;
    }
}
