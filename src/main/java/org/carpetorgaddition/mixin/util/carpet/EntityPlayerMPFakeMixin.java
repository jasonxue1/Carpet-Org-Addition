package org.carpetorgaddition.mixin.util.carpet;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.ThreadContextPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Mixin(value = EntityPlayerMPFake.class, remap = false)
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
        try {
            propagator.setInternal(external);
            return original.call(instance, action, executor);
        } finally {
            propagator.setInternal(false);
        }
    }

    @Inject(method = "lambda$createFake$2", at = @At("RETURN"))
    private static void spawn(CallbackInfo ci, @Local EntityPlayerMPFake fakePlayer) {
        Consumer<EntityPlayerMPFake> consumer = GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.get();
        if (consumer == null) {
            return;
        }
        consumer.accept(fakePlayer);
    }
}
