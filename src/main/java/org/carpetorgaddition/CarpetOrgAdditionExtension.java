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
import org.carpetorgaddition.command.CommandRegister;
import org.carpetorgaddition.command.PlayerManagerCommand;
import org.carpetorgaddition.config.CustomCommandConfig;
import org.carpetorgaddition.config.CustomSettingsConfig;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import org.carpetorgaddition.util.GenericFetcherUtils;
import org.carpetorgaddition.util.permission.PermissionManager;
import org.carpetorgaddition.util.wheel.Translation;
import org.carpetorgaddition.util.wheel.UuidNameMappingTable;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CarpetOrgAdditionExtension implements CarpetExtension {
    private static SettingsManager customSettingManager;

    // 在游戏开始时
    @Override
    public void onGameStarted() {
        // 解析Carpet设置
        SettingsManager settingManager = getCustomSettingManager();
        if (settingManager == null) {
            CarpetServer.settingsManager.parseSettingsClass(CarpetOrgAdditionSettings.class);
        } else {
            settingManager.parseSettingsClass(CarpetOrgAdditionSettings.class);
        }
        UuidNameMappingTable mappingTable = UuidNameMappingTable.getInstance();
        mappingTable.init();
    }

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

    // 当玩家登录时
    @Override
    public void onPlayerLoggedIn(ServerPlayerEntity player) {
        // 假玩家生成时不保留上一次的击退，着火时间，摔落高度
        clearKnockback(player);
        // 提示玩家接收快递
        ExpressManager expressManager = ServerComponentCoordinator.getManager(player.getServer()).getExpressManager();
        expressManager.promptToReceive(player);
        // 加载假玩家安全挂机
        PlayerManagerCommand.loadSafeAfk(player);
        UuidNameMappingTable.getInstance().put(player.getGameProfile());
    }

    /**
     * 清除击退效果
     */
    private static void clearKnockback(ServerPlayerEntity player) {
        if (CarpetOrgAdditionSettings.fakePlayerSpawnNoKnockback && player instanceof EntityPlayerMPFake) {
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
        GenericFetcherUtils.getRuleSelfManager(server).load();
        // 初始化自定义命令名称
        CustomCommandConfig.getInstance().refreshIfExpired();
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        UuidNameMappingTable.getInstance().save();
        PermissionManager.reset();
        CustomCommandConfig.getInstance().refreshIfExpired();
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
