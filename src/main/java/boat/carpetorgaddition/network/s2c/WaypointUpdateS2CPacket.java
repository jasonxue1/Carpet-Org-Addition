package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.PacketUtils;
import boat.carpetorgaddition.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

/**
 * 导航点更新数据包
 *
 * @param target  导航点的目标
 * @param worldId 导航点所在维度
 */
public record WaypointUpdateS2CPacket(Vec3 target, String worldId) implements CustomPacketPayload {
    public static final Type<WaypointUpdateS2CPacket> ID = PacketUtils.createId("waypoint_update");
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointUpdateS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public WaypointUpdateS2CPacket decode(RegistryFriendlyByteBuf buf) {
            long[] arr = buf.readLongArray();
            Vec3 vec3d = new Vec3(arr[0] / 100.0, arr[1] / 100.0, arr[2] / 100.0);
            return new WaypointUpdateS2CPacket(vec3d, buf.readUtf());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, WaypointUpdateS2CPacket value) {
            long[] arr = {
                    (long) (value.target().x() * 100),
                    (long) (value.target().y() * 100),
                    (long) (value.target().z() * 100)};
            buf.writeLongArray(arr);
            buf.writeUtf(value.worldId);
        }
    };

    public WaypointUpdateS2CPacket(Vec3 target, Level world) {
        this(target, WorldUtils.getDimensionId(world));
    }

    public WaypointUpdateS2CPacket(BlockPos blockPos, Level world) {
        this(blockPos.getCenter(), WorldUtils.getDimensionId(world));
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}