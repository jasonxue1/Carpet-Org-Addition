package org.carpetorgaddition.mixin.util.carpet;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.task.batch.BatchSpawnFakePlayerTask;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.ThreadContextPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Mixin(value = EntityPlayerMPFake.class)
public class EntityPlayerMPFakeMixin {
    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<Void> fakePlayerSpawnConsumer(CompletableFuture<T> instance, Consumer<? super T> action, Executor executor, Operation<CompletableFuture<Void>> original) {
        Consumer<EntityPlayerMPFake> onFakePlayerSpawning = GenericUtils.FAKE_PLAYER_SPAWNING.get();
        if (onFakePlayerSpawning == null) {
            return original.call(instance, action, executor);
        }
        Consumer<? super T> consumer = value -> {
            try {
                GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.set(onFakePlayerSpawning);
                action.accept(value);
            } finally {
                GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.remove();
            }
        };
        return original.call(instance, consumer, executor);
    }

    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<Void> fakePlayerLoginMessage(CompletableFuture<T> instance, Consumer<? super T> action, Executor executor, Operation<CompletableFuture<Void>> original) {
        ThreadContextPropagator<Boolean> propagator = CarpetOrgAdditionSettings.hiddenLoginMessages;
        Boolean external = propagator.getExternal();
        Boolean hiddenBatchSpawn = BatchSpawnFakePlayerTask.batchSpawnHiddenMessage.get();
        Consumer<? super T> consumer = value -> {
            try {
                BatchSpawnFakePlayerTask.internalBatchSpawnHiddenMessage.set(hiddenBatchSpawn);
                propagator.setInternal(external);
                action.accept(value);
            } finally {
                propagator.setInternal(false);
                BatchSpawnFakePlayerTask.internalBatchSpawnHiddenMessage.set(false);
            }
        };
        return original.call(instance, consumer, executor);
    }

    @Inject(method = "lambda$createFake$2", at = @At("RETURN"))
    private static void spawn(CallbackInfo ci, @Local EntityPlayerMPFake fakePlayer) {
        Consumer<EntityPlayerMPFake> consumer = GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.get();
        if (consumer == null) {
            return;
        }
        consumer.accept(fakePlayer);
    }

    @WrapOperation(method = "lambda$createFake$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V"))
    private static void onPlayerConnect(PlayerManager instance, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, Operation<Void> original, @Local EntityPlayerMPFake fakePlayer) {
        boolean internal = CarpetOrgAdditionSettings.hiddenLoginMessages.getInternal();
        try {
            original.call(instance, connection, player, clientData);
        } catch (NullPointerException e) {
            if (internal) {
                // 玩家在服务器关闭后登录游戏可能导致服务器崩溃
                CarpetOrgAddition.LOGGER.warn("Fake player attempts to join game after server shutdown", e);
            } else {
                throw e;
            }
        }
    }
}
