package boat.carpetorgaddition;

import boat.carpetorgaddition.command.CommandRegister;
import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.command.SpectatorCommand;
import boat.carpetorgaddition.config.GlobalConfigs;
import boat.carpetorgaddition.logger.LoggerRegister;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.express.ExpressManager;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import boat.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.wheel.GameProfileCache;
import boat.carpetorgaddition.wheel.Translation;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class CarpetOrgAdditionExtension implements CarpetExtension {
    private static boolean settingsLoaded = false;

    // 在游戏开始时
    @Override
    public void onGameStarted() {
        // 解析Carpet设置
        CarpetOrgAdditionSettings.register();
        settingsLoaded = true;
    }

    public static SettingsManager getSettingManager() {
        return CarpetServer.settingsManager;
    }

    public static boolean isCarpetRuleLoaded() {
        return settingsLoaded;
    }

    // 当玩家登录时
    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        // 假玩家生成时不保留上一次的击退，着火时间，摔落高度
        clearKnockback(player);
        // 提示玩家接收快递
        ExpressManager expressManager = ServerComponentCoordinator.getCoordinator(FetcherUtils.getServer(player)).getExpressManager();
        expressManager.promptToReceive(player);
        // 加载假玩家安全挂机
        PlayerManagerCommand.loadSafeAfk(player);
        MinecraftServer server = FetcherUtils.getServer(player);
        GameType gameMode = server.getForcedGameType();
        if (gameMode != null) {
            SpectatorCommand instance = CommandRegister.getCommandInstance(SpectatorCommand.class);
            instance.loadPlayerPos(server, player);
        }
    }

    /**
     * 清除击退效果
     */
    private static void clearKnockback(ServerPlayer player) {
        if (CarpetOrgAdditionSettings.fakePlayerSpawnNoKnockback.get() && player instanceof EntityPlayerMPFake) {
            // 清除速度
            player.setDeltaMovement(Vec3.ZERO);
            // 清除着火时间
            player.setRemainingFireTicks(0);
            // 清除摔落高度
            player.fallDistance = 0;
            // 清除负面效果
            player.getActiveEffects().removeIf(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
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
        GameProfileCache.getInstance().save();
        PermissionManager.reset();
        GlobalConfigs.getInstance().save();
        OfflinePlayerSearchTask.clear();
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
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        CommandRegister.register(dispatcher, access);
    }
}
