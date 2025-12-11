package org.carpetorgaddition.periodic.task.search;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.carpetorgaddition.wheel.traverser.BlockPosTraverser;

import java.util.ArrayList;

public class TradeItemSearchTask extends AbstractTradeSearchTask {
    private final ItemStackPredicate predicate;
    private final Component treadName;

    public TradeItemSearchTask(Level world, BlockPosTraverser blockPosTraverser, BlockPos sourcePos, ItemStackPredicate predicate, CommandSourceStack source) {
        super(world, blockPosTraverser, sourcePos, source);
        this.predicate = predicate;
        this.treadName = predicate.toText();
    }

    @Override
    protected void searchVillager(AbstractVillager merchant) {
        MerchantOffers offers = merchant.getOffers();
        ArrayList<Integer> list = new ArrayList<>();
        for (int index = 0; index < offers.size(); index++) {
            // 检查每个出售的物品是否与谓词匹配
            if (this.predicate.test(offers.get(index).getResult())) {
                list.add(index + 1);
                this.tradeCount++;
            }
        }
        if (list.isEmpty()) {
            return;
        }
        this.results.add(getResult(merchant, list));
        this.villagerCount++;
    }

    /**
     * @return 获取查找结果
     */
    private Result getResult(AbstractVillager merchant, ArrayList<Integer> list) {
        return new Result() {
            @Override
            public int compare(Result o1, Result o2) {
                return MathUtils.compareBlockPos(sourcePos, o1.villagerPos(), o2.villagerPos());
            }

            @Override
            public Component get() {
                // 村民所在坐标
                BlockPos blockPos = merchant.blockPosition();
                // 村民或流浪商人的名称
                Component villagerName = merchant.getName();
                return TextBuilder.translate("carpet.commands.finder.trade.item.each",
                        TextProvider.blockPos(blockPos, ChatFormatting.GREEN), villagerName, getIndexArray(list));
            }

            @Override
            public BlockPos villagerPos() {
                return merchant.blockPosition();
            }
        };
    }

    @Override
    protected void notFound() {
        MessageUtils.sendMessage(this.source,
                "carpet.commands.finder.trade.item.not_trade",
                this.getTradeName(), FinderCommand.VILLAGER);
    }

    @Override
    protected Component getTradeName() {
        return this.treadName;
    }

    @Override
    protected String getTradeResultKey() {
        return "carpet.commands.finder.trade.item.result";
    }
}
