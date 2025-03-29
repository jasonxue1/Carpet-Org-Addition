package org.carpetorgaddition.util.provider;

import java.util.UUID;

public class CommandProvider {
    public static String queryPlayerName(UUID uuid) {
        return "/textclickevent queryPlayerName %s".formatted(uuid.toString());
    }
}
