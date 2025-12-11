package org.carpetorgaddition.mixin.logger;

import carpet.logging.HUDLogger;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.level.gamerules.GameRules;
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

@Mixin(WanderingTraderSpawner.class)
public class WanderingTraderManagerMixin {
    @Shadow
    private int spawnDelay;

    @Shadow
    private int tickDelay;

    @Shadow
    private int spawnChance;

    @Inject(method = "tick", at = @At("HEAD"))
    private void updataLogger(ServerLevel world, boolean spawnMonsters, CallbackInfo ci) {
        if (world.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS)) {
            // 获取流浪商人生成的倒计时，并换算成秒
            int countdown = ((this.spawnDelay == 0 ? 1200 : this.spawnDelay) - (1200 - this.tickDelay)) / 20;
            WanderingTraderSpawnLogger.setSpawnCountdown(new SpawnCountdown(countdown, this.spawnChance));
            return;
        }
        WanderingTraderSpawnLogger.setSpawnCountdown(null);
    }

    @WrapOperation(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;setHomeTo(Lnet/minecraft/core/BlockPos;I)V"))
    private void broadcastSpawnSuccess(WanderingTrader trader, BlockPos pos, int i, Operation<Void> original) {
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
            Component blockPos = TextProvider.blockPos(trader.blockPosition(), ChatFormatting.GREEN);
            Component message = TextBuilder.translate("carpet.logger.wanderingTrader.message", blockPos);
            for (Map.Entry<String, String> entry : entries) {
                ServerPlayer player = server.getPlayerList().getPlayerByName(entry.getKey());
                if (player == null) {
                    continue;
                }
                // 广播流浪商人生成成功
                boolean canNavigate = CommandUtils.canUseCommand(player.createCommandSourceStack(), CarpetOrgAdditionSettings.commandNavigate);
                if (canNavigate) {
                    // 带点击导航的消息
                    Component button = TextBuilder.of("carpet.logger.wanderingTrader.message.navigate")
                            .setCommand(CommandProvider.navigateToUuidEntity(trader.getUUID()))
                            .setHover(TextBuilder.translate("carpet.logger.wanderingTrader.message.navigate.hover", trader.getName()))
                            .setColor(ChatFormatting.AQUA)
                            .build();
                    Component canNavigateMessage = TextBuilder.translate("carpet.logger.wanderingTrader.message.click", blockPos, button);
                    MessageUtils.sendMessage(player, canNavigateMessage);
                } else {
                    MessageUtils.sendMessage(player, message);
                }
                // 播放音效通知流浪商人生成
                WorldUtils.playSound(FetcherUtils.getWorld(trader), player.blockPosition(), trader.getNotifyTradeSound(), trader.getSoundSource());
            }
        }
    }
}
