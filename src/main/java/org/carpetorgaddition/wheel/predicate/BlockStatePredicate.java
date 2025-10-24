package org.carpetorgaddition.wheel.predicate;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.block.*;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import org.carpetorgaddition.mixin.accessor.StateAccessor;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class BlockStatePredicate implements BiPredicate<World, BlockPos> {
    protected final String content;
    private final BiPredicate<World, BlockPos> biPredicate;
    @Nullable
    private final Block block;
    public static final BlockStatePredicate EMPTY = new BlockStatePredicate();

    public BlockStatePredicate(@NotNull Block block) {
        this.content = GenericUtils.getIdAsString(block);
        this.biPredicate = (world, blockPos) -> world.getBlockState(blockPos).isOf(block);
        this.block = block;
    }

    @SuppressWarnings("unused")
    public BlockStatePredicate(BlockState blockState) {
        StringBuilder builder = new StringBuilder();
        Block block = blockState.getBlock();
        builder.append(GenericUtils.getIdAsString(block));
        Map<Property<?>, Comparable<?>> defaultEntries = block.getDefaultState().getEntries();
        List<String> list = new HashMap<>(blockState.getEntries())
                .entrySet()
                .stream()
                // 过滤默认方块状态
                .filter(entry -> !entry.getValue().equals(defaultEntries.get(entry.getKey())))
                .filter(Objects::nonNull)
                .map(StateAccessor.getPropertyMapPrinter())
                .toList();
        if (list.isEmpty()) {
            this.block = block;
        } else {
            this.block = null;
            builder.append("[");
            list.forEach(builder::append);
            builder.append("]");
        }
        this.content = builder.toString();
        this.biPredicate = (world, blockPos) -> world.getBlockState(blockPos).equals(blockState);
    }

    private BlockStatePredicate() {
        this.content = GenericUtils.getIdAsString(Blocks.AIR);
        this.biPredicate = (world, blockPos) -> false;
        this.block = Blocks.AIR;
    }

    private BlockStatePredicate(String content, BiPredicate<World, BlockPos> biPredicate, @Nullable Block block) {
        this.content = content;
        this.biPredicate = biPredicate;
        this.block = block;
    }

    private BlockStatePredicate(String content, BlockStateArgument argument) {
        this.content = content;
        this.biPredicate = (world, blockPos) -> argument.test(new CachedBlockPosition(world, blockPos, false));
        Set<Property<?>> set = argument.getProperties();
        this.block = set.isEmpty() ? argument.getBlockState().getBlock() : null;
    }

    private BlockStatePredicate(LinkedHashSet<Block> blocks) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Block block : blocks) {
            joiner.add(GenericUtils.getIdAsString(block));
        }
        this.content = joiner.toString();
        this.biPredicate = (world, blockPos) -> blocks.contains(world.getBlockState(blockPos).getBlock());
        this.block = null;
    }

    public static BlockStatePredicate ofBlocks(Collection<Block> collection) {
        LinkedHashSet<Block> blocks = new LinkedHashSet<>(collection);
        return switch (blocks.size()) {
            case 0 -> EMPTY;
            case 1 -> new BlockStatePredicate(blocks.getFirst());
            default -> new MatchAnyBlockPredicate(blocks);
        };
    }

    public static BlockStatePredicate ofWorldEater() {
        return new WorldEaterBlockPredicate();
    }

    public static BlockStatePredicate ofPredicate(CommandContext<ServerCommandSource> context, String arguments) {
        for (ParsedCommandNode<ServerCommandSource> commandNode : context.getNodes()) {
            if (commandNode.getNode() instanceof ArgumentCommandNode<?, ?> node && Objects.equals(node.getName(), arguments)) {
                StringRange range = commandNode.getRange();
                String content = context.getInput().substring(range.getStart(), range.getEnd());
                Predicate<CachedBlockPosition> argument;
                try {
                    argument = BlockPredicateArgumentType.getBlockPredicate(context, arguments);
                } catch (CommandSyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
                BiPredicate<World, BlockPos> biPredicate = (world, blockPos) -> argument.test(new CachedBlockPosition(world, blockPos, false));
                Block block = tryConvert(content);
                return new BlockStatePredicate(content, biPredicate, block);
            }
        }
        throw new IllegalArgumentException();
    }

    @SuppressWarnings("unused")
    public static BlockStatePredicate ofState(CommandContext<ServerCommandSource> context, String arguments) {
        for (ParsedCommandNode<ServerCommandSource> commandNode : context.getNodes()) {
            if (commandNode.getNode() instanceof ArgumentCommandNode<?, ?> node && Objects.equals(node.getName(), arguments)) {
                StringRange range = commandNode.getRange();
                String content = context.getInput().substring(range.getStart(), range.getEnd());
                BlockStateArgument argument = BlockStateArgumentType.getBlockState(context, arguments);
                return new BlockStatePredicate(content, argument);
            }
        }
        throw new IllegalArgumentException();
    }

    @Nullable
    private static Block tryConvert(String id) {
        if ("air".equals(id) || "minecraft:air".equals(id)) {
            return Blocks.AIR;
        }
        try {
            Block block = GenericUtils.getBlock(id);
            if (block.getDefaultState().isAir()) {
                return null;
            }
            return block;
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean test(World world, BlockPos blockPos) {
        // 获取区块XZ坐标
        int chunkX = ChunkSectionPos.getSectionCoord(blockPos.getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(blockPos.getZ());
        // 判断区块是否已加载
        if (world.isChunkLoaded(chunkX, chunkZ)) {
            return this.biPredicate.test(world, blockPos);
        }
        return false;
    }

    public Text getDisplayName() {
        if (this == EMPTY) {
            return Blocks.AIR.getName();
        }
        if (this.block != null) {
            return this.block.getName();
        }
        if (this.content.length() > 30) {
            String substring = this.content.substring(0, 30);
            Text ellipsis = TextBuilder.create("...");
            Text result = TextBuilder.combineAll(substring, ellipsis);
            TextBuilder builder = new TextBuilder(result).setGrayItalic().setHover(this.content);
            return builder.build();
        }
        return TextBuilder.create(this.content);
    }

    public static class WorldEaterBlockPredicate extends BlockStatePredicate {
        private WorldEaterBlockPredicate() {
            super();
        }

        @Override
        public boolean test(World world, BlockPos pos) {
            BlockState blockState = world.getBlockState(pos);
            // 排除基岩，空气和流体
            if (blockState.isOf(Blocks.BEDROCK) || blockState.isAir() || blockState.getBlock() instanceof FluidBlock) {
                return false;
            }
            // 被活塞推动时会被破坏
            if (blockState.getPistonBehavior() == PistonBehavior.DESTROY) {
                return false;
            }
            // 高爆炸抗性
            if (blockState.getBlock().getBlastResistance() > 17) {
                return true;
            }
            // 不能推动（实体方块不能被推动）且含水
            boolean blockPiston = blockState.getBlock() instanceof BlockWithEntity || blockState.getPistonBehavior() == PistonBehavior.BLOCK;
            boolean hasWater = !blockState.getFluidState().isEmpty();
            if (blockPiston && hasWater) {
                return true;
            }
            // 含水，可以被推动，但下方8格全都有方块
            return hasWater && canPush(world, pos);
        }

        private boolean canPush(World world, BlockPos pos) {
            for (int i = 1; i <= 8; i++) {
                BlockState blockState = world.getBlockState(pos.down(i));
                // 不可被推动的方块
                PistonBehavior pistonBehavior = blockState.getPistonBehavior();
                if (pistonBehavior == PistonBehavior.BLOCK) {
                    return true;
                }
                // 下方方块可以被推动
                if (pistonBehavior == PistonBehavior.DESTROY) {
                    return false;
                }
            }
            // 下方8格内都有方块
            return true;
        }

        @Override
        public Text getDisplayName() {
            return TextBuilder.translate("carpet.commands.finder.may_affect_world_eater_block.name");
        }
    }

    public static class MatchAnyBlockPredicate extends BlockStatePredicate {
        private final LinkedHashSet<Block> blocks;

        private MatchAnyBlockPredicate(LinkedHashSet<Block> blocks) {
            super(blocks);
            this.blocks = blocks;
        }

        @Override
        public Text getDisplayName() {
            if (this.content.length() > 30) {
                String display = this.content.substring(0, 30) + "...";
                TextBuilder builder = new TextBuilder(display);
                StringJoiner joiner = new StringJoiner(",\n");
                for (Block block : this.blocks) {
                    joiner.add(GenericUtils.getIdAsString(block));
                }
                return builder
                        .setGrayItalic()
                        .setStringHover(joiner.toString())
                        .build();
            }
            return TextBuilder.create(this.content);
        }
    }
}
