package org.carpetorgaddition.periodic.navigator;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;

public class BlockPosNavigator extends AbstractNavigator {
    protected final BlockPos blockPos;
    protected final World world;

    public BlockPosNavigator(@NotNull ServerPlayerEntity player, BlockPos blockPos, World world) {
        super(player);
        this.blockPos = blockPos;
        this.world = world;
        // 同步导航点
        this.syncWaypoint(new WaypointUpdateS2CPacket(blockPos.toCenterPos(), world));
    }

    @Override
    public void tick() {
        if (this.shouldTerminate()) {
            this.clear();
            return;
        }
        MutableText text;
        if (this.player.getWorld().equals(this.world)) {
            MutableText in = TextProvider.simpleBlockPos(this.blockPos);
            MutableText distance = TextBuilder.translate(DISTANCE, MathUtils.getBlockIntegerDistance(this.player.getBlockPos(), this.blockPos));
            text = getHUDText(this.blockPos.toCenterPos(), in, distance);
        } else {
            text = TextBuilder.combineAll(TextProvider.getDimensionName(this.world), TextProvider.simpleBlockPos(this.blockPos));
        }
        MessageUtils.sendMessageToHud(this.player, text);
    }

    @Override
    public BlockPosNavigator copy(ServerPlayerEntity player) {
        return new BlockPosNavigator(player, this.blockPos, this.world);
    }

    @Override
    protected boolean shouldTerminate() {
        // 玩家与目的地在同一维度
        if (this.player.getWorld().equals(this.world)) {
            if (MathUtils.getBlockIntegerDistance(this.player.getBlockPos(), this.blockPos) <= 8) {
                // 到达目的地，停止追踪
                MessageUtils.sendMessageToHud(this.player, TextBuilder.translate(REACH));
                this.clear();
                return true;
            }
        }
        return false;
    }
}
