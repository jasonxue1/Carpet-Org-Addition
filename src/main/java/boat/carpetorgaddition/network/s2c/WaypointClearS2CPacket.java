package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.network.PacketUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

/**
 * 导航点清除数据包
 */
public final class WaypointClearS2CPacket implements CustomPacketPayload {
    public static final Type<WaypointClearS2CPacket> ID = PacketUtils.createId("waypoint_clear");
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointClearS2CPacket> CODEC = StreamCodec.ofMember((ignore, ignore0) -> CarpetOrgAddition.pass(), ignore1 -> new WaypointClearS2CPacket());
    public static final WaypointClearS2CPacket INSTANCE = new WaypointClearS2CPacket();

    private WaypointClearS2CPacket() {
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
