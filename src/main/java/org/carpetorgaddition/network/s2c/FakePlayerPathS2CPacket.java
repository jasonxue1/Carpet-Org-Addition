package org.carpetorgaddition.network.s2c;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.network.PacketUtils;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerPathfinder;

import java.util.ArrayList;
import java.util.List;


// TODO 隐藏功能不在未启用的情况下注册
public record FakePlayerPathS2CPacket(int id, List<Vec3d> list) implements CustomPayload {
    public static final CustomPayload.Id<FakePlayerPathS2CPacket> ID = PacketUtils.createId("fake_player_path");

    public static final PacketCodec<RegistryByteBuf, FakePlayerPathS2CPacket> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, FakePlayerPathS2CPacket value) {
            buf.writeInt(value.id());
            buf.writeCollection(value.list(), VEC3D_CODEC);
        }

        @Override
        public FakePlayerPathS2CPacket decode(RegistryByteBuf buf) {
            int id = buf.readInt();
            ArrayList<Vec3d> list = buf.readCollection(ArrayList::new, VEC3D_CODEC);
            return new FakePlayerPathS2CPacket(id, list);
        }
    };

    private static final PacketCodec<PacketByteBuf, Vec3d> VEC3D_CODEC = new PacketCodec<>() {
        @Override
        public Vec3d decode(PacketByteBuf buf) {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            return new Vec3d(x, y, z);
        }

        @Override
        public void encode(PacketByteBuf buf, Vec3d value) {
            buf.writeDouble(value.getX());
            buf.writeDouble(value.getY());
            buf.writeDouble(value.getZ());
        }
    };

    public FakePlayerPathS2CPacket(FakePlayerPathfinder pathfinder) {
        this(pathfinder.getFakePlayer().getId(), pathfinder.getRenderNodes());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
