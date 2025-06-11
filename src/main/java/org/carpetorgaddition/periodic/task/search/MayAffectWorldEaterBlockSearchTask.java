package org.carpetorgaddition.periodic.task.search;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.carpetorgaddition.wheel.SelectionArea;
import org.carpetorgaddition.wheel.TextBuilder;

public class MayAffectWorldEaterBlockSearchTask extends BlockSearchTask {
    public MayAffectWorldEaterBlockSearchTask(ServerWorld world, BlockPos sourcePos, SelectionArea selectionArea, CommandContext<ServerCommandSource> context, FinderCommand.BlockPredicate blockPredicate) {
        super(world, sourcePos, selectionArea, context, blockPredicate);
    }

    @Override
    protected MutableText getResultMessage(BlockPos sourcteBlockPos, BlockPos blockPos) {
        return TextBuilder.translate(
                "carpet.commands.finder.may_affect_world_eater_block.feedback",
                MathUtils.getBlockIntegerDistance(sourcteBlockPos, blockPos),
                TextProvider.blockPos(blockPos, Formatting.GREEN),
                this.world.getBlockState(blockPos).getBlock().getName()
        );
    }
}
