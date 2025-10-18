package org.carpetorgaddition.periodic.navigator;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.network.s2c.WaypointClearS2CPacket;
import org.carpetorgaddition.periodic.PlayerComponentCoordinator;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.Waypoint;
import org.jetbrains.annotations.Nullable;

public class NavigatorManager {
    @Nullable
    private AbstractNavigator navigator;
    private boolean isUpdated = false;
    private final ServerPlayerEntity player;

    public NavigatorManager(ServerPlayerEntity player) {
        this.player = player;
    }

    public void tick() {
        if (this.navigator == null) {
            return;
        }
        try {
            if (this.navigator.isArrive()) {
                this.clearNavigator();
            } else {
                if (this.isUpdated) {
                    this.isUpdated = false;
                    this.navigator.onStart();
                }
                this.navigator.tick();
            }
        } catch (RuntimeException e) {
            MessageUtils.sendErrorMessage(this.player.getCommandSource(), e, "carpet.commands.navigate.exception");
            CarpetOrgAddition.LOGGER.error("导航器没有按照预期工作", e);
            // 清除导航器
            this.clearNavigator();
        }
    }

    @Nullable
    public AbstractNavigator getNavigator() {
        return this.navigator;
    }

    public void setNavigator(Entity entity, boolean isContinue) {
        this.setNavigator(new EntityNavigator(this.player, entity, isContinue));
    }

    public void setNavigator(Waypoint waypoint) {
        this.setNavigator(new WaypointNavigator(this.player, waypoint));
    }

    public void setNavigator(BlockPos blockPos, World world) {
        this.setNavigator(new BlockPosNavigator(this.player, blockPos, world));
    }

    public void setNavigator(BlockPos blockPos, World world, Text name) {
        this.setNavigator(new HasNamePosNavigator(this.player, blockPos, world, name));
    }

    private void setNavigator(@Nullable AbstractNavigator navigator) {
        ServerPlayNetworking.send(this.player, WaypointClearS2CPacket.INSTANCE);
        this.navigator = navigator;
        this.isUpdated = true;
    }

    public void clearNavigator() {
        this.setNavigator((AbstractNavigator) null);
        ServerPlayNetworking.send(this.player, WaypointClearS2CPacket.INSTANCE);
    }

    public void setNavigatorFromOldPlayer(ServerPlayerEntity oldPlayer) {
        NavigatorManager manager = PlayerComponentCoordinator.getManager(oldPlayer).getNavigatorManager();
        AbstractNavigator navigator = manager.getNavigator();
        this.navigator = navigator == null ? null : navigator.copy(this.player);
    }
}
