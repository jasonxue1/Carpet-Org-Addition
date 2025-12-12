package boat.carpetorgaddition.client.logger;


import boat.carpetorgaddition.network.s2c.LoggerUpdateS2CPacket;

import java.util.HashMap;

@SuppressWarnings("unused")
public class ClientLogger {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final HashMap<String, String> subscriptions = new HashMap<>();

    private static void put(String logger, String option) {
        subscriptions.put(logger, option);
    }

    private static void remove(String logger) {
        subscriptions.remove(logger);
        onRemove(logger);
    }

    public static void onPacketReceive(LoggerUpdateS2CPacket packet) {
        if (packet.isRemove()) {
            remove(packet.logName());
        } else {
            put(packet.logName(), packet.option());
        }
    }

    private static void onRemove(String logger) {
    }
}
