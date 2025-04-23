package org.carpetorgaddition.command;

import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.config.CommandsConfig;

public class CommandConstants {
    // 命令的默认名称
    public static final String DEFAULT_CREEPER_COMMAND = "creeper";
    public static final String DEFAULT_FINDER_COMMAND = "finder";
    public static final String DEFAULT_ITEM_SHADOWING_COMMAND = "itemshadowing";
    public static final String DEFAULT_KILL_ME_COMMAND = "killMe";
    public static final String DEFAULT_LOCATIONS_COMMAND = "locations";
    public static final String DEFAULT_MAIL_COMMAND = "mail";
    public static final String DEFAULT_NAVIGATE_COMMAND = "navigate";
    public static final String DEFAULT_PLAYER_ACTION_COMMAND = "playerAction";
    public static final String DEFAULT_PLAYER_MANAGER_COMMAND = "playerManager";
    public static final String DEFAULT_RULE_SEARCH_COMMAND = "ruleSearch";
    public static final String DEFAULT_SPECTATOR_COMMAND = "spectator";
    public static final String DEFAULT_XP_TRANSFER_COMMAND = "xpTransfer";
    public static final String DEFAULT_CARPET_ORG_ADDITION_COMMAND = CarpetOrgAddition.MOD_ID;
    public static final String DEFAULT_DICTIONARY_COMMAND = "dictionary";
    public static final String DEFAULT_HIGHLIGHT_COMMAND = "highlight";

    // 命令的自定义名称
    public static final String CREEPER_COMMAND = getCommand(DEFAULT_CREEPER_COMMAND);
    public static final String FINDER_COMMAND = getCommand(DEFAULT_FINDER_COMMAND);
    public static final String ITEM_SHADOWING_COMMAND = getCommand(DEFAULT_ITEM_SHADOWING_COMMAND);
    public static final String KILL_ME_COMMAND = getCommand(DEFAULT_KILL_ME_COMMAND);
    public static final String LOCATIONS_COMMAND = getCommand(DEFAULT_LOCATIONS_COMMAND);
    public static final String MAIL_COMMAND = getCommand(DEFAULT_MAIL_COMMAND);
    public static final String NAVIGATE_COMMAND = getCommand(DEFAULT_NAVIGATE_COMMAND);
    public static final String PLAYER_ACTION_COMMAND = getCommand(DEFAULT_PLAYER_ACTION_COMMAND);
    public static final String PLAYER_MANAGER_COMMAND = getCommand(DEFAULT_PLAYER_MANAGER_COMMAND);
    public static final String RULE_SEARCH_COMMAND = getCommand(DEFAULT_RULE_SEARCH_COMMAND);
    public static final String SPECTATOR_COMMAND = getCommand(DEFAULT_SPECTATOR_COMMAND);
    public static final String XP_TRANSFER_COMMAND = getCommand(DEFAULT_XP_TRANSFER_COMMAND);
    public static final String CARPET_ORG_ADDITION_COMMAND = getCommand(DEFAULT_CARPET_ORG_ADDITION_COMMAND);
    public static final String DICTIONARY_COMMAND = getCommand(DEFAULT_DICTIONARY_COMMAND);
    public static final String HIGHLIGHT_COMMAND = getCommand(DEFAULT_HIGHLIGHT_COMMAND);

    private static String getCommand(String command) {
        CommandsConfig config = CommandsConfig.getInstance();
        return config.getCommand(command);
    }
}
