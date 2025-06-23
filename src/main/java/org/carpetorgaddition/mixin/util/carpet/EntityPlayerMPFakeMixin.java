package org.carpetorgaddition.mixin.util.carpet;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import org.carpetorgaddition.util.GenericUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(value = EntityPlayerMPFake.class, remap = false)
public class EntityPlayerMPFakeMixin {
    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;whenCompleteAsync(Ljava/util/function/BiConsumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<T> whenCompleteAsync(CompletableFuture<Optional<GameProfile>> instance, BiConsumer<? super T, ? super Throwable> action, Executor executor, Operation<CompletableFuture<T>> original) {
        Consumer<EntityPlayerMPFake> onFakePlayerSpawning = GenericUtils.FAKE_PLAYER_SPAWNING.get();
        if (onFakePlayerSpawning == null) {
            return original.call(instance, action, executor);
        }
        BiConsumer<? super T, Throwable> biConsumer = (value, throwable) -> {
            try {
                GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.set(onFakePlayerSpawning);
                action.accept(value, throwable);
            } finally {
                GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.remove();
            }
        };
        return original.call(instance, biConsumer, executor);
    }

    @Inject(method = "lambda$createFake$2", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;getAbilities()Lnet/minecraft/entity/player/PlayerAbilities;"))
    private static void spawn(CallbackInfo ci, @Local EntityPlayerMPFake fakePlayer) {
        Consumer<EntityPlayerMPFake> consumer = GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.get();
        if (consumer == null) {
            return;
        }
        consumer.accept(fakePlayer);
    }
}
