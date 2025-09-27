package org.carpetorgaddition.mixin.command;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import org.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mixin(DataResult.Error.class)
public class DataResultErrorMixin<R> {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void init(Supplier<String> messageSupplier, Optional<R> partialValue, Lifecycle lifecycle, CallbackInfo ci) {
        UUID uuid = OfflinePlayerSearchTask.CURRENT_UUID.get();
        if (uuid == null) {
            return;
        }
        OfflinePlayerSearchTask.addCorruptedPlayerUUID(uuid);
    }
}
