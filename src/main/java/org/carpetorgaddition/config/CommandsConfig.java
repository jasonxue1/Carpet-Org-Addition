package org.carpetorgaddition.config;

import org.carpetorgaddition.command.CommandConstants;
import org.carpetorgaddition.util.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class CommandsConfig {
    // TODO 改为json格式，可以为一条命令设置多个名称
    private final File file = IOUtils.createConfigFile("commands.properties", false);
    private final Properties properties = new Properties();
    private static final CommandsConfig INSTANCE = new CommandsConfig();

    public static CommandsConfig getInstance() {
        return INSTANCE;
    }

    private CommandsConfig() {
        if (this.file.isFile()) {
            this.load();
        } else {
            this.init();
        }
    }

    /**
     * 初始化配置文件
     */
    private void init() {
        try (BufferedWriter writer = IOUtils.toWriter(this.file)) {
            this.initProperties();
            this.properties.store(writer, "Edit to modify command name");
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
    }

    /**
     * 加载配置文件
     */
    private void load() {
        try (BufferedReader reader = IOUtils.toReader(this.file)) {
            properties.load(reader);
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
    }

    /**
     * 获取命令的自定义名称，如果不存在，返回参数本身做为默认值
     */
    public String getCommand(String command) {
        return this.properties.getProperty(command + "Command", command);
    }

    /**
     * 初始化所有命令默认名称
     */
    private void initProperties() {
        this.initProperty(CommandConstants.DEFAULT_CREEPER_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_FINDER_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_ITEM_SHADOWING_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_KILL_ME_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_LOCATIONS_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_MAIL_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_NAVIGATE_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_PLAYER_ACTION_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_PLAYER_MANAGER_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_RULE_SEARCH_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_SPECTATOR_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_XP_TRANSFER_COMMAND);
        this.initProperty(CommandConstants.DEFAULT_CARPET_ORG_ADDITION_COMMAND);
    }

    /**
     * 初始化单个命令名称
     */
    private void initProperty(String command) {
        this.properties.setProperty(command + "Command", command);
    }
}
