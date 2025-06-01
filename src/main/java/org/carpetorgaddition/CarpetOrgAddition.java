package org.carpetorgaddition;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.carpetorgaddition.config.CustomSettingsConfig;
import org.carpetorgaddition.debug.DebugRuleRegistrar;
import org.carpetorgaddition.network.NetworkS2CPacketRegister;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.wheel.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CarpetOrgAddition implements ModInitializer {
    /**
     * 日志
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("CarpetOrgAddition");
    /**
     * 模组ID
     */
    public static final String MOD_ID = "carpet-org-addition";
    /**
     * 模组元数据
     */
    public static final ModMetadata METADATA = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
    /**
     * 模组名称
     */
    public static final String MOD_NAME = METADATA.getName();
    /**
     * 模组当前的版本
     */
    public static final String VERSION = METADATA.getVersion().getFriendlyString();
    /**
     * 模组名称小写
     */
    public static final String MOD_NAME_LOWER_CASE = MOD_NAME.replace(" ", "").toLowerCase(Locale.ROOT);
    /**
     * 模组的构建时间戳
     */
    public static final String BUILD_TIMESTAMP = METADATA.getCustomValue("buildTimestamp").getAsString();
    /**
     * 当前jvm是否为调试模式
     */
    public static final boolean IS_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(s -> s.contains("jdwp"));
    /**
     * 是否同时加载了{@code Lithium}（锂）模组
     */
    public static final boolean LITHIUM = FabricLoader.getInstance().isModLoaded("lithium");
    /**
     * 是否启用隐藏功能
     */
    public static final boolean ENABLE_HIDDEN_FUNCTION = Boolean.getBoolean("CarpetOrgAddition.EnableHiddenFunction");
    /**
     * 是否允许自定义规则管理器
     */
    public static final boolean ALLOW_CUSTOM_SETTINGS_MANAGER = false;

    /**
     * 模组初始化
     */
    @Override
    public void onInitialize() {
        if (ALLOW_CUSTOM_SETTINGS_MANAGER) {
            CustomSettingsConfig.initSettingsManagerConfigs();
        }
        CarpetServer.manageExtension(new CarpetOrgAdditionExtension());
        // 注册网络数据包
        NetworkS2CPacketRegister.register();
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            CarpetOrgAddition.LOGGER.info("Hidden feature enabled");
        }
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            this.runs();
        }
        // 如果当前为调试模式的开发环境，注册测试规则
        if (isDebugDevelopment()) {
            DebugRuleRegistrar.getInstance().registrar();
            CarpetOrgAddition.LOGGER.info("Build timestamp: {}", BUILD_TIMESTAMP);
        }
    }

    /**
     * 记录游戏的启动次数
     */
    private void runs() {
        File file = new File("../startlog.txt");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            // 自有记录以来的启动次数
            int total = 0;
            Counter<LocalDate> counter = new Counter<>();
            if (file.isFile()) {
                // 读取历史启动次数
                List<String> list = Files.readAllLines(file.toPath());
                for (String str : list) {
                    if (str.isEmpty()) {
                        continue;
                    }
                    String[] split = str.split("=");
                    if (split.length != 2) {
                        continue;
                    }
                    LocalDate localDate = LocalDate.parse(split[0], formatter);
                    int count = Integer.parseInt(split[1]);
                    counter.add(localDate, count);
                    total += count;
                }
            }
            LocalDate now = LocalDate.now();
            total++;
            counter.add(now);
            // 获取今天的启动次数
            int count = counter.getCount(now);
            CarpetOrgAddition.LOGGER.info("The game has been launched {} times today", count);
            // 保存启动次数
            List<LocalDate> list = counter.keySet().stream().sorted().toList();
            // 重新写入前备份文件，写入完毕后删除备份
            File backup = IOUtils.backup(file, false);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            try (writer) {
                for (LocalDate date : list) {
                    writer.write(date.format(formatter) + "=" + counter.getCount(date));
                    writer.newLine();
                }
            }
            IOUtils.removeFile(backup);
            String earliest = list.getLast().format(formatter);
            CarpetOrgAddition.LOGGER.info("The game has been launched a total of {} times since {}", total, earliest);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.warn("An unexpected error occurred while recording the number of game launches", e);
        }
    }

    /**
     * @return 当前环境是否为调试模式的开发环境
     */
    public static boolean isDebugDevelopment() {
        return IS_DEBUG && FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
