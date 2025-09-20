package org.carpetorgaddition.mixin.logger;

import carpet.logging.HUDLogger;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.WanderingTraderManager;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.logger.LoggerRegister;
import org.carpetorgaddition.logger.Loggers;
import org.carpetorgaddition.logger.WanderingTraderSpawnLogger;
import org.carpetorgaddition.logger.WanderingTraderSpawnLogger.SpawnCountdown;
import org.carpetorgaddition.mixin.accessor.carpet.LoggerAccessor;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

@Mixin(WanderingTraderManager.class)
public class WanderingTraderManagerMixin {
    @Shadow
    private int spawnDelay;

    @Shadow
    private int spawnTimer;

    @Shadow
    private int spawnChance;

    @Inject(method = "spawn", at = @At("HEAD"))
    private void updataLogger(ServerWorld world, boolean spawnMonsters, CallbackInfo ci) {
        if (world.getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING)) {
            // 获取流浪商人生成的倒计时，并换算成秒
            int countdown = ((this.spawnDelay == 0 ? 1200 : this.spawnDelay) - (1200 - this.spawnTimer)) / 20;
            WanderingTraderSpawnLogger.setSpawnCountdown(new SpawnCountdown(countdown, this.spawnChance));
            return;
        }
        WanderingTraderSpawnLogger.setSpawnCountdown(null);
    }

    @WrapOperation(method = "trySpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WanderingTraderEntity;setPositionTarget(Lnet/minecraft/util/math/BlockPos;I)V"))
    private void broadcastSpawnSuccess(WanderingTraderEntity trader, BlockPos pos, int i, Operation<Void> original) {
        original.call(trader, pos, i);
        if (LoggerRegister.wanderingTrader && WanderingTraderSpawnLogger.spawnCountdownNonNull()) {
            // 获取流浪商人所在的服务器
            MinecraftServer server = FetcherUtils.getServer(trader);
            if (server == null) {
                return;
            }
            HUDLogger logger = Loggers.getWanderingTraderLogger();
            Set<Map.Entry<String, String>> entries = ((LoggerAccessor) logger).getSubscribedOnlinePlayers().entrySet();
            // 普通消息
            Text blockPos = TextProvider.blockPos(trader.getBlockPos(), Formatting.GREEN);
            Text message = TextBuilder.translate("carpet.logger.wanderingTrader.message", blockPos);
            for (Map.Entry<String, String> entry : entries) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player == null) {
                    continue;
                }
                // 广播流浪商人生成成功
                boolean canNavigate = CommandUtils.canUseCommand(player.getCommandSource(), CarpetOrgAdditionSettings.commandNavigate);
                if (canNavigate) {
                    // 带点击导航的消息
                    Text button = TextBuilder.of("carpet.logger.wanderingTrader.message.navigate")
                            .setCommand(CommandProvider.navigateToUuidEntity(trader.getUuid()))
                            .setHover(TextBuilder.translate("carpet.logger.wanderingTrader.message.navigate.hover", trader.getName()))
                            .setColor(Formatting.AQUA)
                            .build();
                    Text canNavigateMessage = TextBuilder.translate("carpet.logger.wanderingTrader.message.click", blockPos, button);
                    MessageUtils.sendMessage(player, canNavigateMessage);
                } else {
                    MessageUtils.sendMessage(player, message);
                }
                // 播放音效通知流浪商人生成
                WorldUtils.playSound(FetcherUtils.getWorld(trader), player.getBlockPos(), trader.getYesSound(), trader.getSoundCategory());
            }
        }
    }
}
