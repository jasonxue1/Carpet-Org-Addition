package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.carpetorgaddition.network.PacketUtils;
import org.carpetorgaddition.util.GenericUtils;

/**
 * 导航点清除数据包
 */
public final class WaypointClearS2CPacket implements CustomPayload {
    public static final Id<WaypointClearS2CPacket> ID = PacketUtils.createId("waypoint_clear");
    public static final PacketCodec<RegistryByteBuf, WaypointClearS2CPacket> CODEC = PacketCodec.of((buf, value) -> GenericUtils.pass(), buf -> new WaypointClearS2CPacket());
    public static final WaypointClearS2CPacket INSTANCE = new WaypointClearS2CPacket();

    private WaypointClearS2CPacket() {
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
