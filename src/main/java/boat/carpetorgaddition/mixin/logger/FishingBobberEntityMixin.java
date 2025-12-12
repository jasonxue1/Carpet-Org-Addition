package boat.carpetorgaddition.mixin.logger;

import boat.carpetorgaddition.logger.FunctionLogger;
import boat.carpetorgaddition.logger.LoggerRegister;
import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.TextBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingBobberEntityMixin {
    @Shadow
    @Nullable
    public abstract Player getPlayerOwner();

    @Shadow
    private int nibble;

    @Shadow
    private int timeUntilLured;

    @Shadow
    private int timeUntilHooked;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (LoggerRegister.fishing) {
            FunctionLogger logger = Loggers.getFishingLogger();
            if (this.getPlayerOwner() instanceof ServerPlayer player && logger.isSubscribed(player)) {
                if (this.timeUntilLured > 0) {
                    // 鱼出现
                    MessageUtils.sendMessageToHud(player, TextBuilder.translate("carpet.logger.fishing.appear", this.timeUntilLured));
                } else if (this.timeUntilHooked > 0) {
                    // 鱼上钩
                    MessageUtils.sendMessageToHud(player, TextBuilder.translate("carpet.logger.fishing.bite", this.timeUntilHooked));
                } else if (this.nibble > 0) {
                    // 鱼挣脱
                    TextBuilder builder = TextBuilder.of("carpet.logger.fishing.break_free", this.nibble);
                    builder.setColor(ChatFormatting.GREEN);
                    MessageUtils.sendMessageToHud(player, builder.build());
                }
            }
        }
    }
}
