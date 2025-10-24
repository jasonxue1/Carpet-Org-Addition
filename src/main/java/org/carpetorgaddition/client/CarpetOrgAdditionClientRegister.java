package org.carpetorgaddition.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.client.command.ClientCommandRegister;
import org.carpetorgaddition.client.logger.ClientLogger;
import org.carpetorgaddition.client.renderer.waypoint.NavigatorWaypoint;
import org.carpetorgaddition.client.renderer.waypoint.Waypoint;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRenderer;
import org.carpetorgaddition.debug.client.render.HudDebugRendererRegister;
import org.carpetorgaddition.network.s2c.*;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.screen.BackgroundSpriteSyncSlot;
import org.carpetorgaddition.wheel.screen.UnavailableSlotImplInterface;

public class CarpetOrgAdditionClientRegister {

    public static void register() {
        registerCommand();
        registerC2SNetworkPack();
        registerNetworkPackReceiver();
        registerRenderer();
        registerKeyBinding();
        developed();
    }

    /**
     * 注册客户端命令
     */
    private static void registerCommand() {
        ClientCommandRegister.register();
    }

    /**
     * 注册客户端到服务端的数据包
     */
    private static void registerC2SNetworkPack() {
    }

    /**
     * 注册数据包接收器
     */
    private static void registerNetworkPackReceiver() {
        // 注册路径点更新数据包
        ClientPlayNetworking.registerGlobalReceiver(
                WaypointUpdateS2CPacket.ID,
                (payload, context) -> {
                    WaypointRenderer instance = WaypointRenderer.getInstance();
                    Vec3d target = payload.target();
                    RegistryKey<World> registryKey = GenericUtils.getWorld(payload.worldId());
                    Waypoint waypoint = instance.addOrUpdate(new NavigatorWaypoint(registryKey, target));
                    waypoint.setTarget(registryKey, target);
                }
        );
        // 注册路径点清除数据包
        ClientPlayNetworking.registerGlobalReceiver(
                WaypointClearS2CPacket.ID,
                (payload, context) -> {
                    WaypointRenderer instance = WaypointRenderer.getInstance();
                    instance.listRenderers(Waypoint.NAVIGATOR).forEach(instance::stop);
                }
        );
        // 容器不可用槽位同步数据包
        ClientPlayNetworking.registerGlobalReceiver(UnavailableSlotSyncS2CPacket.ID, (payload, context) -> {
            ScreenHandler screen = context.player().currentScreenHandler;
            if (screen.syncId == payload.syncId() && screen instanceof UnavailableSlotImplInterface anInterface) {
                anInterface.carpet_Org_Addition$sync(payload);
            }
        });
        // 背景精灵同步数据包
        ClientPlayNetworking.registerGlobalReceiver(BackgroundSpriteSyncS2CPacket.ID, (payload, context) -> {
            ScreenHandler screen = context.player().currentScreenHandler;
            if (screen.syncId == payload.syncId() && screen.getSlot(payload.slotIndex()) instanceof BackgroundSpriteSyncSlot slot) {
                slot.carpet_Org_Addition$setIdentifier(payload.identifier());
            }
        });
        // 记录器更新数据包
        ClientPlayNetworking.registerGlobalReceiver(LoggerUpdateS2CPacket.ID, (packet, context) -> ClientLogger.onPacketReceive(packet));
    }

    /**
     * 注册渲染器
     */
    private static void registerRenderer() {
        // 注册路径点渲染器
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> WaypointRenderer.getInstance().render(context));
    }

    /**
     * 注册按键绑定
     */
    private static void registerKeyBinding() {
        // 清除高亮路径点
        KeyBindingHelper.registerKeyBinding(CarpetOrgAdditionClient.CLEAR_WAYPOINT);
    }

    /**
     * 仅用于开发测试
     */
    private static void developed() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            HudDebugRendererRegister.register();
        }
    }
}
