package org.carpetorgaddition.periodic.task.search;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.wheel.BlockRegion;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.provider.TextProvider;

public class MayAffectWorldEaterBlockSearchTask extends BlockSearchTask {
    public MayAffectWorldEaterBlockSearchTask(ServerWorld world, BlockPos sourcePos, BlockRegion blockRegion, ServerCommandSource source, FinderCommand.BlockPredicate blockPredicate) {
        super(world, sourcePos, blockRegion, source, blockPredicate);
    }

    @Override
    protected Text getResultMessage(BlockPos sourcteBlockPos, BlockPos blockPos) {
        return TextBuilder.translate(
                "carpet.commands.finder.may_affect_world_eater_block.feedback",
                MathUtils.getBlockIntegerDistance(sourcteBlockPos, blockPos),
                TextProvider.blockPos(blockPos, Formatting.GREEN),
                this.world.getBlockState(blockPos).getBlock().getName()
        );
    }
}
