package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.task.batch.BatchSpawnFakePlayerTask;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(YggdrasilGameProfileRepository.class)
public class YggdrasilGameProfileRepositoryMixin {
    @WrapOperation(method = "findProfileByName", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private void suppressionWarning(Logger instance, String key, Object name, Object e, Operation<Void> original) {
        if (BatchSpawnFakePlayerTask.REQUEST.orElse(false)) {
            return;
        }
        original.call(instance, key, name, e);
    }
}
