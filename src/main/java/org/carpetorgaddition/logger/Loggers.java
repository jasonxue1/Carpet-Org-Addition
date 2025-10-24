package org.carpetorgaddition.logger;

import carpet.logging.HUDLogger;
import carpet.logging.LoggerRegistry;

public class Loggers {
    /**
     * 流浪商人生成记录器
     */
    public static HUDLogger getWanderingTraderLogger() {
        return (HUDLogger) LoggerRegistry.getLogger(LoggerNames.WANDERING_TRADER_SPAWN_COUNTDOWN);
    }

    /**
     * 钓鱼记录器
     */
    public static FunctionLogger getFishingLogger() {
        return (FunctionLogger) LoggerRegistry.getLogger(LoggerNames.FISHING);
    }

    /**
     * 黑曜石生成记录器
     */
    public static FunctionLogger getObsidianLogger() {
        return (FunctionLogger) LoggerRegistry.getLogger(LoggerNames.OBSIDIAN);
    }
}
