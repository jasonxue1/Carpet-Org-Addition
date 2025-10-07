package org.carpetorgaddition.periodic.task.search;

import net.minecraft.block.Block;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.BlockRegion;
import org.carpetorgaddition.wheel.BlockStatePredicate;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.page.PageManager;
import org.carpetorgaddition.wheel.page.PagedCollection;
import org.carpetorgaddition.wheel.provider.TextProvider;

import java.util.*;
import java.util.function.Supplier;

public class BlockSearchTask extends ServerTask {
    protected final ServerWorld world;
    private final BlockRegion blockRegion;
    protected final ServerCommandSource source;
    protected final BlockPos sourcePos;
    private Iterator<BlockPos> iterator;
    private FindState findState;
    /**
     * tick方法开始执行时的时间
     */
    private long startTime;
    /**
     * 任务被执行的总游戏刻数
     */
    private int tickCount;
    private final BlockStatePredicate predicate;
    /**
     * 已经迭代过的坐标集合
     */
    private final Set<BlockPos> blockPosCache = Collections.newSetFromMap(new WeakHashMap<>());
    private int count = 0;
    private final ArrayList<Result> results = new ArrayList<>();
    private final PagedCollection pagedCollection;

    public BlockSearchTask(ServerWorld world, BlockPos sourcePos, BlockRegion blockRegion, ServerCommandSource source, BlockStatePredicate predicate) {
        this.world = world;
        this.sourcePos = sourcePos;
        this.blockRegion = blockRegion;
        this.source = source;
        this.predicate = predicate;
        this.findState = FindState.SEARCH;
        this.tickCount = 0;
        PageManager pageManager = FetcherUtils.getPageManager(source.getServer());
        this.pagedCollection = pageManager.newPagedCollection(this.source);
    }

    @Override
    public void tick() {
        this.startTime = System.currentTimeMillis();
        this.tickCount++;
        if (this.tickCount > FinderCommand.MAX_TICK_COUNT) {
            // 任务超时
            MessageUtils.sendErrorMessage(this.source, FinderCommand.TIME_OUT);
            this.findState = FindState.END;
            return;
        }
        while (true) {
            if (this.timeout()) {
                return;
            }
            try {
                switch (this.findState) {
                    case SEARCH -> this.searchBlock();
                    case SORT -> this.sort();
                    case FEEDBACK -> this.sendFeedback();
                    default -> {
                        return;
                    }
                }
            } catch (TaskExecutionException e) {
                e.disposal();
                this.findState = FindState.END;
                return;
            }
        }
    }

    // 查找方块
    private void searchBlock() {
        if (this.iterator == null) {
            this.iterator = this.blockRegion.iterator();
        }
        while (this.iterator.hasNext()) {
            if (this.timeout()) {
                return;
            }
            BlockPos blockPos = this.iterator.next();
            if (this.blockPosCache.contains(blockPos)) {
                continue;
            }
            this.iterate(blockPos);
        }
        this.findState = FindState.SORT;
    }

    private void iterate(BlockPos begin) {
        HashMap<Block, Set<BlockPos>> group = new HashMap<>();
        BlockPos.iterateRecursively(begin, Integer.MAX_VALUE, Integer.MAX_VALUE, (pos, consumer) -> {
            for (Direction direction : Direction.values()) {
                consumer.accept(pos.offset(direction));
            }
        }, blockPos -> {
            if (this.predicate.test(this.world, blockPos) && this.blockPosCache.add(blockPos) && this.blockRegion.contains(blockPos)) {
                // 缓存方块坐标到软引用集合，下次不再迭代这个坐标
                Block block = world.getBlockState(blockPos).getBlock();
                Set<BlockPos> set = group.computeIfAbsent(block, ignore -> new HashSet<>());
                // 如果软引用blockPosCache集合中的内容被回收，则此处可能重复执行
                if (set.add(blockPos)) {
                    this.count++;
                    if (this.count > FinderCommand.MAXIMUM_STATISTICAL_COUNT) {
                        // 方块过多，无法统计
                        Runnable function = () -> MessageUtils.sendErrorMessage(
                                this.source,
                                "carpet.commands.finder.block.too_much_blocks",
                                this.predicate.getDisplayName()
                        );
                        throw new TaskExecutionException(function);
                    }
                } else {
                    CarpetOrgAddition.LOGGER.debug("Repeatedly adding iterated block position: {}", blockPos);
                }
                return true;
            }
            return false;
        });
        for (Map.Entry<Block, Set<BlockPos>> entry : group.entrySet()) {
            this.results.add(new Result(entry.getKey(), entry.getValue()));
        }
    }

    // 对结果排序
    private void sort() {
        if (this.results.isEmpty()) {
            // 从周围没有找到指定方块
            Text name = this.predicate.getDisplayName();
            MessageUtils.sendMessage(this.source, "carpet.commands.finder.block.not_found_block", name);
            this.findState = FindState.END;
            return;
        }
        this.results.sort((o1, o2) -> {
            int compare = Integer.compare(o1.set.size(), o2.set.size());
            if (compare == 0) {
                return MathUtils.compareBlockPos(this.sourcePos, o1.centerPos(), o2.centerPos());
            }
            return -compare;
        });
        this.findState = FindState.FEEDBACK;
    }

    // 发送反馈
    protected void sendFeedback() {
        int count = this.results.size();
        Text name = this.predicate.getDisplayName();
        MessageUtils.sendEmptyMessage(this.source);
        MessageUtils.sendMessage(this.source, "carpet.commands.finder.block.find", count, name);
        this.pagedCollection.addContent(this.results);
        CommandUtils.handlingException(this.pagedCollection::print, this.source);
        this.findState = FindState.END;
    }

    // 当前任务是否超时
    private boolean timeout() {
        return (System.currentTimeMillis() - this.startTime) > FinderCommand.MAX_FIND_TIME;
    }

    @Override
    public boolean stopped() {
        return this.findState == FindState.END;
    }

    public class Result implements Supplier<Text> {
        private final BlockPos center;
        private final Block block;
        private final Set<BlockPos> set;

        private Result(Block block, Set<BlockPos> set) {
            this.center = MathUtils.calculateTheGeometricCenter(set);
            this.block = block;
            this.set = set;
        }

        @Override
        public Text get() {
            return getResultMessage(this.block, this.set);
        }

        public BlockPos centerPos() {
            return center;
        }
    }

    private Text getResultMessage(Block block, Set<BlockPos> set) {
        BlockPos center = MathUtils.calculateTheGeometricCenter(set);
        TextBuilder builder = TextBuilder.of(
                "carpet.commands.finder.block.feedback",
                TextProvider.blockPos(center),
                set.size(),
                block.getName()
        );
        return builder.build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            return Objects.equals(this.source.getPlayer(), ((BlockSearchTask) obj).source.getPlayer());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.source.getPlayer());
    }

    public enum FindState {
        SEARCH,
        SORT,
        FEEDBACK,
        END
    }

    @Override
    public String getLogName() {
        return "方块查找";
    }
}
