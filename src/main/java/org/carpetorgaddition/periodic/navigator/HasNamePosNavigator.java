package org.carpetorgaddition.periodic.navigator;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;

public class HasNamePosNavigator extends BlockPosNavigator {
    private final Text name;

    public HasNamePosNavigator(@NotNull ServerPlayerEntity player, BlockPos blockPos, World world, Text name) {
        super(player, blockPos, world);
        this.name = name;
    }

    @Override
    public void tick() {
        if (this.shouldTerminate()) {
            this.clear();
            return;
        }
        MutableText text;
        MutableText posText = TextProvider.simpleBlockPos(this.blockPos);
        // 玩家与目的地是否在同一维度
        if (this.player.getWorld().equals(this.world)) {
            MutableText distance = TextBuilder.translate(DISTANCE, MathUtils.getBlockIntegerDistance(this.player.getBlockPos(), this.blockPos));
            text = getHUDText(this.blockPos.toCenterPos(), TextBuilder.translate(IN, this.name, posText), distance);
        } else {
            text = TextBuilder.translate(IN, this.name, TextBuilder.combineAll(TextProvider.getDimensionName(this.world), posText));
        }
        MessageUtils.sendMessageToHud(this.player, text);
    }

    @Override
    public HasNamePosNavigator copy(ServerPlayerEntity player) {
        return new HasNamePosNavigator(player, this.blockPos, this.world, this.name);
    }
}
