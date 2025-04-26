package org.carpetorgaddition.rule.value;

import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAdditionSettings;

public enum OpenPlayerInventory {
    FALSE,
    FAKE_PLAYER,
    ONLINE_PLAYER;

    public static boolean isEnable(ServerCommandSource ignored) {
        return CarpetOrgAdditionSettings.playerCommandOpenPlayerInventory != FALSE;
    }
}
