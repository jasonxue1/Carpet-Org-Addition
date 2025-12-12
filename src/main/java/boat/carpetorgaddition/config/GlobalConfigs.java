package boat.carpetorgaddition.config;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.dataupdate.DataUpdater;
import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class GlobalConfigs {
    /**
     * 配置文件的路径
     */
    private final File configFile = IOUtils.CONFIGURE_DIRECTORY.resolve(CarpetOrgAddition.MOD_ID + ".json").toFile();
    private final HashMap<Class<?>, AbstractConfig<?>> configurations = new HashMap<>();
    private final JsonObject json;
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
            JsonObject json;
            try {
                json = IOUtils.loadJson(this.configFile);
            } catch (IOException e) {
                json = this.initJsonObject();
            }
            this.json = json;
        } else {
            this.json = this.initJsonObject();
        }
        this.register();
    }

    private JsonObject initJsonObject() {
        CarpetOrgAddition.LOGGER.info("Initializing configuration file");
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
        return json;
    }

    @SuppressWarnings("unchecked")
    public <E extends JsonElement, C extends AbstractConfig<E>> void load(C config) {
        JsonElement element = this.json.get(config.getKey());
        try {
            config.load((E) element);
        } catch (RuntimeException e) {
            IOUtils.backupFile(this.configFile);
            this.json.add(config.getKey(), config.getJsonValue());
            CarpetOrgAddition.LOGGER.warn("Global config partially corrupted - resetting damaged section", e);
        }
    }

    public void save() {
        try {
            for (AbstractConfig<?> configuration : this.configurations.values()) {
                if (configuration.shouldBeSaved()) {
                    this.json.add(configuration.getKey(), configuration.getJsonValue());
                }
            }
            IOUtils.write(this.configFile, this.json);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("An unexpected error occurred while saving the global configuration file for {}", CarpetOrgAddition.MOD_NAME, e);
        }
    }

    private void register() {
        this.register(new CustomCommandConfig(this));
        this.register(new HiddenFunctionConfig(this));
    }

    private void register(AbstractConfig<?> config) {
        this.configurations.put(config.getClass(), config);
        this.load(config);
    }

    private <T extends AbstractConfig<?>> T getConfig(Class<T> key) {
        AbstractConfig<?> config = this.configurations.get(key);
        return key.cast(config);
    }

    /**
     * @return 是否启用了隐藏功能
     */
    public boolean isEnableHiddenFunction() {
        return this.getConfig(HiddenFunctionConfig.class).isEnable();
    }

    public String[] getCommand(String command) {
        return this.getConfig(CustomCommandConfig.class).getCommand(command);
    }
}
