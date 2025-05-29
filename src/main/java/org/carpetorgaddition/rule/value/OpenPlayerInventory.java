package org.carpetorgaddition.rule.value;

import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAdditionSettings;

public enum OpenPlayerInventory {
    FALSE(false, false, false),
    FAKE_PLAYER(true, false, false),
    ONLINE_PLAYER(true, true, false),
    NON_WHITELIST(true, false, true),
    ALL_PLAYER(true, true, true);

    private final boolean fake;
    private final boolean real;
    private final boolean offline;

    OpenPlayerInventory(boolean fake, boolean real, boolean offline) {
        this.fake = fake;
        this.real = real;
        this.offline = offline;
    }

    /**
     * @return 是否允许打开假玩家的物品栏
     */
    public boolean canOpenFakePlayer() {
        return this.fake;
    }

    /**
     * @return 是否可以打开真玩家的物品栏
     */
    public boolean canOpenRealPlayer() {
        return this.real;
    }

    /**
     * @return 是否允许打开离线玩家的物品栏
     */
    public boolean canOpenOfflinePlayer() {
        return this.offline;
    }

    /**
     * @return 打开该玩家物品栏是否需要权限
     */
    public boolean permissionRequired() {
        return this == NON_WHITELIST;
    }

    public static boolean isEnable(ServerCommandSource ignored) {
        return CarpetOrgAdditionSettings.playerCommandOpenPlayerInventory != FALSE;
    }
}
