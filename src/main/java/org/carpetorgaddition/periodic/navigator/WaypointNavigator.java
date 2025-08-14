package org.carpetorgaddition.periodic.navigator;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.Waypoint;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.jetbrains.annotations.NotNull;

public class WaypointNavigator extends AbstractNavigator {
    private final Waypoint waypoint;
    /**
     * 路径点所在维度的ID
     */
    private final World world;
    /**
     * 玩家所在的上一个维度
     */
    private World previousWorld;

    public WaypointNavigator(@NotNull ServerPlayerEntity player, Waypoint waypoint) {
        super(player);
        this.waypoint = waypoint;
        if (this.waypoint.getBlockPos() == null) {
            throw new NullPointerException();
        }
        this.world = waypoint.getWorld();
    }

    @Override
    public void tick() {
        if (shouldTerminate()) {
            this.clear();
            return;
        }
        World playerWorld = this.player.getEntityWorld();
        // 路径点的目标位置
        BlockPos targetPos = this.waypoint.getBlockPos();
        // 玩家所在的方块位置
        BlockPos playerPos = this.player.getBlockPos();
        // 玩家所在维度
        if (this.world == playerWorld) {
            // 玩家和路径点在相同的维度
            Text text = this.getHUDText(targetPos.toCenterPos(), getIn(targetPos), getDistance(playerPos, targetPos));
            MessageUtils.sendMessageToHud(this.player, text);
        } else {
            BlockPos anotherPos = this.waypoint.getAnotherBlockPos();
            if (WorldUtils.canMappingPos(this.world, playerWorld) && anotherPos != null) {
                // 玩家和路径点在不同的维度，但是两个世界的坐标可以互相转换
                TextBuilder builder = new TextBuilder(TextProvider.simpleBlockPos(anotherPos));
                // 将坐标设置为斜体
                builder.setItalic();
                Text in = TextBuilder.translate(IN, waypoint.getName(), builder.build());
                Text text = this.getHUDText(anotherPos.toCenterPos(), in, getDistance(playerPos, anotherPos));
                MessageUtils.sendMessageToHud(this.player, text);
                targetPos = anotherPos;
            } else {
                // 玩家和路径点在不同维度
                Text dimensionName = TextProvider.dimension(WorldUtils.getWorld(this.player.getServer(), this.waypoint.getWorldAsString()));
                MutableText in = TextBuilder.translate(IN, waypoint.getName(), TextBuilder.combineAll(dimensionName, TextProvider.simpleBlockPos(targetPos)));
                MessageUtils.sendMessageToHud(this.player, in);
            }
        }
        if (playerWorld != this.previousWorld) {
            this.previousWorld = playerWorld;
            this.syncWaypoint(new WaypointUpdateS2CPacket(targetPos, world));
        }
    }

    @Override
    public boolean shouldTerminate() {
        if (this.player.getEntityWorld() == this.world && MathUtils.getBlockIntegerDistance(this.player.getBlockPos(), this.waypoint.getBlockPos()) <= 8) {
            // 到达目的地，停止追踪
            MessageUtils.sendMessageToHud(this.player, TextBuilder.translate(REACH));
            this.clear();
            return true;
        }
        return false;
    }

    @Override
    public WaypointNavigator copy(ServerPlayerEntity player) {
        if (this.waypoint.getBlockPos() == null) {
            return null;
        }
        return new WaypointNavigator(player, this.waypoint);
    }

    @NotNull
    private MutableText getIn(BlockPos blockPos) {
        return TextBuilder.translate(IN, waypoint.getName(), TextProvider.simpleBlockPos(blockPos));
    }

    @NotNull
    private static MutableText getDistance(BlockPos playerBlockPos, BlockPos blockPos) {
        return TextBuilder.translate(DISTANCE, MathUtils.getBlockIntegerDistance(playerBlockPos, blockPos));
    }
}
