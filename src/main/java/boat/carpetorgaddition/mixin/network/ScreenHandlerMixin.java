package boat.carpetorgaddition.mixin.network;

import boat.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.screen.UnavailableSlotImplInterface;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractContainerMenu.class, priority = 1001)
public class ScreenHandlerMixin implements UnavailableSlotImplInterface {
    @Shadow
    @Final
    public int containerId;
    @Unique
    private int from = 0;
    @Unique
    private int to = -1;

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int button, ClickType actionType, Player player, CallbackInfo ci) {
        if (MathUtils.isInRange(this.from, this.to, slotIndex)) {
            ci.cancel();
        }
    }

    @Override
    public void carpet_Org_Addition$sync(UnavailableSlotSyncS2CPacket pack) {
        if (this.containerId == pack.syncId()) {
            this.from = pack.from();
            this.to = pack.to();
        }
    }
}
