package boat.carpetorgaddition.network;

import boat.carpetorgaddition.network.c2s.ObjectSearchTaskC2SPacket;
import boat.carpetorgaddition.network.handler.ObjectSearchTaskPacketHandler;
import boat.carpetorgaddition.network.s2c.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class NetworkS2CPacketRegister {
    /**
     * 注册数据包
     */
    public static void register() {
        // 更新导航点数据包
        PayloadTypeRegistry.playS2C().register(WaypointUpdateS2CPacket.ID, WaypointUpdateS2CPacket.CODEC);
        // 清除导航点数据包
        PayloadTypeRegistry.playS2C().register(WaypointClearS2CPacket.ID, WaypointClearS2CPacket.CODEC);
        // 容器禁用槽位同步数据包
        PayloadTypeRegistry.playS2C().register(UnavailableSlotSyncS2CPacket.ID, UnavailableSlotSyncS2CPacket.CODEC);
        // 背景精灵同步数据包
        PayloadTypeRegistry.playS2C().register(BackgroundSpriteSyncS2CPacket.ID, BackgroundSpriteSyncS2CPacket.CODEC);
        // 记录器更新数据包
        PayloadTypeRegistry.playS2C().register(LoggerUpdateS2CPacket.ID, LoggerUpdateS2CPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ObjectSearchTaskC2SPacket.ID, ObjectSearchTaskC2SPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ObjectSearchTaskC2SPacket.ID, new ObjectSearchTaskPacketHandler());
    }
}
