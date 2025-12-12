package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.exception.CCEUpdateSuppressException;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.ReportedException;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public class PacketApplyBatcherMixin {
    @SuppressWarnings("unchecked")
    @WrapOperation(method = "handle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"))
    private static <T extends PacketListener> void exceptionReason(final Packet<T> packet, final T listener, Operation<Void> original) {
        try {
            original.call(packet, listener);
        } catch (RuntimeException e) {
            CCEUpdateSuppressException cce;
            if (e instanceof CCEUpdateSuppressException) {
                cce = (CCEUpdateSuppressException) e;
            } else if (e instanceof ReportedException crashException && crashException.getCause() instanceof CCEUpdateSuppressException exception) {
                cce = exception;
            } else {
                throw e;
            }
            exceptionReason((Packet<ServerGamePacketListener>) packet, listener, cce);
            throw e;
        }
    }

    // 如果是玩家操作导致的，记录异常原因
    @Unique
    private static <T extends PacketListener> void exceptionReason(Packet<ServerGamePacketListener> packet, T listener, CCEUpdateSuppressException cceUpdateSuppressException) {
        if (listener instanceof ServerGamePacketListenerImpl networkHandler) {
            cceUpdateSuppressException.onCatch(networkHandler.player, packet);
        }
    }
}
