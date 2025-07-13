package org.carpetorgaddition.config;

import com.google.gson.JsonObject;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.util.IOUtils;

import java.io.File;
import java.io.IOException;

public class CarpetOrgAdditionConfigs {
    // TODO 处理文件找不到的情况
    private static final File CONFIG = IOUtils.CONFIGURE_DIRECTORY.resolve(CarpetOrgAddition.MOD_ID + ".json").toFile();

    static {
        // 静态代码块必须放在成员变量赋值之后
        init();
    }

    /**
     * 初始化配置文件
     */
    private static void init() {
        if (CONFIG.isFile()) {
            return;
        }
        CarpetOrgAddition.LOGGER.info("Initializing configuration file");
        try {
            JsonObject json = new JsonObject();
            json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
            IOUtils.saveJson(CONFIG, json);
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
    }

    /**
     * @return 是否启用了隐藏功能
     */
    public static boolean isEnableHiddenFunction() {
        try {
            JsonObject json = IOUtils.loadJson(CONFIG);
            if (json.has(ConfigKeys.ENABLE_HIDDEN_FUNCTION)) {
                return json.get(ConfigKeys.ENABLE_HIDDEN_FUNCTION).getAsBoolean();
            }
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
        return false;
    }
}
