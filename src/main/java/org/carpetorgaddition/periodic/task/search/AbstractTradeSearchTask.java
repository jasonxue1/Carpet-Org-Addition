package org.carpetorgaddition.periodic.task.search;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.page.PageManager;
import org.carpetorgaddition.wheel.page.PagedCollection;
import org.carpetorgaddition.wheel.traverser.BlockPosTraverser;

import java.util.*;
import java.util.function.Supplier;

public abstract class AbstractTradeSearchTask extends ServerTask {
    protected final World world;
    protected final BlockPosTraverser blockPosTraverser;
    protected final BlockPos sourcePos;
    protected Iterator<MerchantEntity> iterator;
    private final PagedCollection pagedCollection;
    private FindState findState;
    protected final ArrayList<Result> results = new ArrayList<>();
    /**
     * 周围村民的数量，一般情况下等于this.results.size()，只在个别情况下例外，例如一个村民出售了至少3本相同附魔的附魔书，并且其中两本附魔书等级也相同
     */
    protected int villagerCount;
    /**
     * 交易选择的总数量
     */
    protected int tradeCount;

    public AbstractTradeSearchTask(World world, BlockPosTraverser blockPosTraverser, BlockPos sourcePos, ServerCommandSource source) {
        super(source);
        this.world = world;
        this.blockPosTraverser = blockPosTraverser;
        this.sourcePos = sourcePos;
        this.findState = FindState.SEARCH;
        PageManager pageManager = FetcherUtils.getPageManager(source.getServer());
        this.pagedCollection = pageManager.newPagedCollection(this.source);
    }

    @Override
    public void tick() {
        this.checkTimeout();
        while (this.isTimeRemaining()) {
            try {
                switch (this.findState) {
                    case SEARCH -> searchVillager();
                    case SORT -> sort();
                    case FEEDBACK -> feedback();
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

    // 查找周围的村民
    private void searchVillager() {
        if (this.iterator == null) {
            this.iterator = this.world.getNonSpectatingEntities(MerchantEntity.class, this.blockPosTraverser.toBox()).iterator();
        }
        while (this.iterator.hasNext()) {
            if (this.isTimeExpired()) {
                return;
            }
            MerchantEntity merchant = this.iterator.next();
            // 检查每一只村民交易
            this.searchVillager(merchant);
        }
        // 没有找到交易物品，跳过排序直接结束任务
        if (this.results.isEmpty()) {
            this.finish();
            return;
        }
        this.findState = FindState.SORT;
    }

    protected abstract void searchVillager(MerchantEntity merchant);

    protected abstract void notFound();

    protected abstract Text getTradeName();

    // 对结果进行排序
    private void sort() {
        this.results.sort((o1, o2) -> o1.compare(o1, o2));
        this.findState = FindState.FEEDBACK;
    }

    // TODO 命令反馈错误，额外的附魔书文本
    private void feedback() {
        ArrayList<Object> list = new ArrayList<>();
        // 村民数量
        list.add(this.villagerCount);
        list.add(this.getTradeName());
        list.add(FinderCommand.VILLAGER);
        // 总交易选项数量
        list.add(this.tradeCount);
        // 消息的翻译键
        String key = "carpet.commands.finder.trade.result";
        MessageUtils.sendEmptyMessage(this.source);
        // 发送消息：在周围找到了<交易选项数量>个出售<出售的物品名称>的<村民>或<流浪商人>
        MessageUtils.sendMessage(this.source, key, list.toArray(Object[]::new));
        this.pagedCollection.addContent(this.results);
        CommandUtils.handlingException(this.pagedCollection::print, this.source);
        this.findState = FindState.END;
    }

    @Override
    public boolean stopped() {
        return this.findState == FindState.END;
    }

    /**
     * 因没找到交易物品而停止查找
     */
    private void finish() {
        this.notFound();
        this.findState = FindState.END;
    }

    @Override
    public long getMaxExecutionTime() {
        return FinderCommand.MAX_SEARCH_TIME;
    }

    @Override
    protected long getMaxTimeSlice() {
        return FinderCommand.TIME_SLICE;
    }

    public interface Result extends Comparator<Result>, Supplier<Text> {
        BlockPos villagerPos();
    }

    /**
     * @param list 一只村民所有符合条件交易的索引
     * @return 索引拼接后的字符串
     */
    protected static String getIndexArray(ArrayList<Integer> list) {
        String indexArray;
        // 如果只有一个索引，直接返回元素字符串
        if (list.size() == 1) {
            return list.getFirst().toString();
        }
        // 如果多个索引，将索引拼接后返回
        StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
        for (Integer index : list) {
            stringJoiner.add(index.toString());
        }
        indexArray = stringJoiner.toString();
        return indexArray;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AbstractTradeSearchTask that) {
            return Objects.equals(source, that.source);
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
        return "交易查找";
    }
}
