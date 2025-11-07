package org.carpetorgaddition.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.util.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class GlobalConfigs {
    /**
     * 配置文件的路径
     */
    private final File configFile = IOUtils.CONFIGURE_DIRECTORY.resolve(CarpetOrgAddition.MOD_ID + ".json").toFile();
    private final HashSet<AbstractConfig<?>> configurations = new HashSet<>();
    private final CustomCommandConfig customCommandNames = new CustomCommandConfig();
    private boolean updateRequired = false;
    /**
     * 是否启用隐藏功能
     */
    private static final String ENABLE_HIDDEN_FUNCTION = "enableHiddenFunction";
    private static volatile GlobalConfigs INSTANCE;

    /**
     * @apiNote 如果使用饿汉式单例，则可能因为类加载顺序问题抛出空指针异常
     */
    @NotNull
    public static GlobalConfigs getInstance() {
        if (INSTANCE == null) {
            synchronized (GlobalConfigs.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GlobalConfigs();
                }
            }
        }
        return INSTANCE;
    }

    private GlobalConfigs() {
        if (this.configFile.isFile()) {
            return;
        }
        CarpetOrgAddition.LOGGER.info("Initializing configuration file");
        try {
            JsonObject json = new JsonObject();
            json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
            IOUtils.write(this.configFile, json);
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
        this.register();
    }

    /**
     * @return 是否启用了隐藏功能
     */
    public boolean isEnableHiddenFunction() {
        try {
            JsonObject json = IOUtils.loadJson(this.configFile);
            return json.has(ENABLE_HIDDEN_FUNCTION) && json.get(ENABLE_HIDDEN_FUNCTION).getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    public void save() {
        try {
            File file = this.configFile;
            JsonObject json = IOUtils.loadJson(file);
            for (AbstractConfig<?> configuration : this.configurations) {
                json.add(configuration.getKey(), configuration.getJsonValue());
            }
            IOUtils.write(file, json);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("An unexpected error occurred while saving the global configuration file for {}", CarpetOrgAddition.MOD_NAME, e);
        }
        this.updateRequired = false;
    }

    /**
     * 标记为需要更新
     */
    public void markUpdateRequired() {
        this.updateRequired = true;
    }

    public CustomCommandConfig getCustomCommandNames() {
        return this.customCommandNames;
    }

    @SuppressWarnings("unchecked")
    public <E extends JsonElement, C extends AbstractConfig<E>> void load(C config) {
        JsonObject json;
        try {
            json = IOUtils.loadJson(this.configFile);
        } catch (IOException e) {
            IOUtils.loggerError(e);
            return;
        }
        JsonElement element = json.get(config.getKey());
        try {
            config.load((E) element);
        } catch (RuntimeException e) {
            IOUtils.backupFile(this.configFile);
            json.add(config.getKey(), config.getJsonValue());
            this.markUpdateRequired();
            CarpetOrgAddition.LOGGER.warn("Global config partially corrupted - resetting damaged section", e);
        }
    }

    private void register() {
        this.register(this.customCommandNames);
    }

    private <E extends JsonElement, C extends AbstractConfig<E>> void register(C config) {
        this.configurations.add(config);
        this.load(config);
        if (this.updateRequired) {
            this.save();
        }
    }
}
