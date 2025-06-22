package org.carpetorgaddition.debug;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import net.fabricmc.loader.api.FabricLoader;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.exception.ProductionEnvironmentError;

import java.util.HashMap;
import java.util.Map;

public class DebugRuleRegistrar implements CarpetExtension {
    private final SettingsManager settingsManager;
    private static DebugRuleRegistrar instance;
    private static final HashMap<String, String> TRANSLATIONS = new HashMap<>();

    public static DebugRuleRegistrar getInstance() {
        if (instance == null) {
            instance = new DebugRuleRegistrar();
        }
        return instance;
    }

    private DebugRuleRegistrar() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            this.settingsManager = new SettingsManager(
                    CarpetOrgAddition.VERSION,
                    "carpet-debug",
                    "Carpet Org Addition Debug"
            );
        } else {
            throw new ProductionEnvironmentError();
        }
    }

    public void registrar() {
        CarpetServer.manageExtension(this);
    }

    @Override
    public SettingsManager extensionSettingsManager() {
        return this.settingsManager;
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return TRANSLATIONS;
    }
}
