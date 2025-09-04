package org.carpetorgaddition;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.carpetorgaddition.command.CommandRegister;
import org.carpetorgaddition.command.PlayerManagerCommand;
import org.carpetorgaddition.command.SpectatorCommand;
import org.carpetorgaddition.config.CustomSettingsConfig;
import org.carpetorgaddition.config.GlobalConfigs;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.Translation;
import org.carpetorgaddition.wheel.UuidNameMappingTable;
import org.carpetorgaddition.wheel.permission.PermissionManager;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CarpetOrgAdditionExtension implements CarpetExtension {
    private static SettingsManager customSettingManager;
    private static boolean settingsLoaded = false;

    // 在游戏开始时
    @Override
    public void onGameStarted() {
        // 解析Carpet设置
        SettingsManager settingManager = getCustomSettingManager();
        if (settingManager == null) {
            CarpetServer.settingsManager.parseSettingsClass(CarpetOrgAdditionSettings.class);
            // TODO 是否应移动到if-else后
            CarpetOrgAdditionSettings.register();
            settingsLoaded = true;
        } else {
            settingManager.parseSettingsClass(CarpetOrgAdditionSettings.class);
        }
        UuidNameMappingTable mappingTable = UuidNameMappingTable.getInstance();
        mappingTable.init();
    }

    @Deprecated
    @Nullable
    public static SettingsManager getCustomSettingManager() {
        if (CarpetOrgAddition.ALLOW_CUSTOM_SETTINGS_MANAGER && customSettingManager == null) {
            try {
                customSettingManager = CustomSettingsConfig.getSettingManager();
            } catch (RuntimeException e) {
                return null;
            }
        }
        return customSettingManager;
    }

    public static SettingsManager getSettingManager() {
        SettingsManager settingManager = getCustomSettingManager();
        return settingManager == null ? CarpetServer.settingsManager : settingManager;
    }

    public static boolean isCarpetRuleLoaded() {
        return settingsLoaded;
    }

    // 当玩家登录时
    @Override
    public void onPlayerLoggedIn(ServerPlayerEntity player) {
        // 假玩家生成时不保留上一次的击退，着火时间，摔落高度
        clearKnockback(player);
        // 提示玩家接收快递
        ExpressManager expressManager = ServerComponentCoordinator.getManager(GenericUtils.getServer(player)).getExpressManager();
        expressManager.promptToReceive(player);
        // 加载假玩家安全挂机
        PlayerManagerCommand.loadSafeAfk(player);
        UuidNameMappingTable.getInstance().put(player.getGameProfile());
        MinecraftServer server = GenericUtils.getServer(player);
        GameMode gameMode = server.getForcedGameMode();
        if (gameMode != null) {
            SpectatorCommand instance = CommandRegister.getCommandInstance(SpectatorCommand.class);
            instance.loadPlayerPos(server, player);
        }
    }

    /**
     * 清除击退效果
     */
    private static void clearKnockback(ServerPlayerEntity player) {
        if (CarpetOrgAdditionSettings.fakePlayerSpawnNoKnockback.get() && player instanceof EntityPlayerMPFake) {
            // 清除速度
            player.setVelocity(Vec3d.ZERO);
            // 清除着火时间
            player.setFireTicks(0);
            // 清除摔落高度
            player.fallDistance = 0;
            // 清除负面效果
            player.getStatusEffects().removeIf(effect -> effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL);
        }
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        // 玩家自动登录
        FakePlayerSerializer.autoLogin(server);
        PermissionManager.load(server);
        FetcherUtils.getRuleSelfManager(server).load();
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        UuidNameMappingTable.getInstance().save();
        PermissionManager.reset();
        GlobalConfigs.save();
    }

    @Override
    public SettingsManager extensionSettingsManager() {
        if (CarpetOrgAddition.ALLOW_CUSTOM_SETTINGS_MANAGER) {
            SettingsManager settingManager = CarpetOrgAdditionExtension.getCustomSettingManager();
            if (settingManager == CarpetServer.settingsManager) {
                return CarpetExtension.super.extensionSettingsManager();
            }
            return settingManager;
        }
        return CarpetExtension.super.extensionSettingsManager();
    }

    // 设置模组翻译
    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return Translation.getInstance().getTranslation();
    }

    // 注册记录器
    @Override
    public void registerLoggers() {
        LoggerRegister.register();
    }

    // 注册命令
    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        CommandRegister.register(dispatcher, access);
    }
}
