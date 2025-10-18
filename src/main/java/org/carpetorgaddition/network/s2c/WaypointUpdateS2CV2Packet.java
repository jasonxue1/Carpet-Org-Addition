package org.carpetorgaddition.network.s2c;

import net.minecraft.entity.Entity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.network.PacketUtils;
import org.carpetorgaddition.util.FetcherUtils;

/**
 * 导航点更新数据包
 *
 * @param target      导航点的目标
 * @param registryKey 导航点所在维度
 */
public record WaypointUpdateS2CV2Packet(Vec3d target, int id, RegistryKey<World> registryKey) implements CustomPayload {
    public static final Id<WaypointUpdateS2CV2Packet> ID = PacketUtils.createId("waypoint_update_v2");
    public static final PacketCodec<RegistryByteBuf, WaypointUpdateS2CV2Packet> CODEC = new PacketCodec<>() {
        @Override
        public WaypointUpdateS2CV2Packet decode(RegistryByteBuf buf) {
            Vec3d vec3d = buf.readVec3d();
            int id = buf.readInt();
            RegistryKey<World> key = buf.readRegistryKey(RegistryKeys.WORLD);
            return new WaypointUpdateS2CV2Packet(vec3d, id, key);
        }

        @Override
        public void encode(RegistryByteBuf buf, WaypointUpdateS2CV2Packet value) {
            buf.writeVec3d(value.target);
            buf.writeInt(value.id);
            buf.writeRegistryKey(value.registryKey);
        }
    };

    public WaypointUpdateS2CV2Packet(Vec3d target, int id, World world) {
        this(target, id, world.getRegistryKey());
    }

    public WaypointUpdateS2CV2Packet(Entity entity) {
        this(entity.getEyePos(), entity.getId(), FetcherUtils.getWorld(entity).getRegistryKey());
    }

    public WaypointUpdateS2CV2Packet(BlockPos blockPos, World world) {
        this(blockPos.toCenterPos(), -1, world.getRegistryKey());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
