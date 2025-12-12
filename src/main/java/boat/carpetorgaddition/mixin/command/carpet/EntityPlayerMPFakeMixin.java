package boat.carpetorgaddition.mixin.command.carpet;

import boat.carpetorgaddition.mixin.command.ServerPlayerEntityMixin;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMPFake.class)
public abstract class EntityPlayerMPFakeMixin extends ServerPlayerEntityMixin {
    @Unique
    private boolean isDead = false;

    @Inject(method = "die", at = @At("HEAD"))
    private void onDeath(DamageSource cause, CallbackInfo ci) {
        this.isDead = true;
    }

    @Override
    public boolean carpet_Org_Addition$afkTriggerFail() {
        return this.isDead;
    }
}
