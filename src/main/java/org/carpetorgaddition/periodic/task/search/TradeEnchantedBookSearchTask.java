package org.carpetorgaddition.periodic.task.search;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.World;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.predicate.EnchantedBookPredicate;
import org.carpetorgaddition.wheel.provider.TextProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TradeEnchantedBookSearchTask extends AbstractTradeSearchTask {
    private final EnchantedBookPredicate predicate;

    public TradeEnchantedBookSearchTask(World world, BlockPosTraverser blockPosTraverser, BlockPos sourcePos, ServerCommandSource source, EnchantedBookPredicate predicate) {
        super(world, blockPosTraverser, sourcePos, source);
        this.predicate = predicate;
    }

    @Override
    protected void searchVillager(MerchantEntity merchant) {
        TradeOfferList offers = merchant.getOffers();
        // 键：附魔书的等级，值：同一只村民出售的相同附魔书的索引集合
        HashMap<Integer, ArrayList<Integer>> hashMap = new HashMap<>();
        for (int index = 0; index < offers.size(); index++) {
            ItemStack enchantedBook = offers.get(index).getSellItem();
            // 获取每个交易结果槽上的附魔书附魔等级
            int level = getBookEnchantment(enchantedBook);
            if (level == -1) {
                continue;
            }
            // 将同一只村民出售的相同附魔相同等级的附魔书分到一组
            ArrayList<Integer> result = hashMap.get(level);
            if (result == null) {
                ArrayList<Integer> list = new ArrayList<>();
                list.add(index + 1);
                hashMap.put(level, list);
            } else {
                // 同一只村民出售了多本相同附魔书，将结果组装起来
                result.add(index + 1);
            }
            this.tradeCount++;
        }
        if (hashMap.isEmpty()) {
            return;
        }
        // 添加结果
        for (Map.Entry<Integer, ArrayList<Integer>> entry : hashMap.entrySet()) {
            this.results.add(new EnchantedBookFindResult(merchant, entry.getValue(), entry.getKey()));
        }
        this.villagerCount++;
    }

    private int getBookEnchantment(ItemStack book) {
        if (book.isOf(Items.ENCHANTED_BOOK)) {
            return this.predicate.getLevel(book);
        }
        return -1;
    }

    @Override
    protected void notFound() {
        MessageUtils.sendMessage(this.source,
                "carpet.commands.finder.trade.find.not_trade",
                this.getTradeName(), FinderCommand.VILLAGER);
    }

    @Override
    protected Text getTradeName() {
        return this.predicate.getDisplayName();
    }

    public class EnchantedBookFindResult implements Result {
        private final MerchantEntity merchant;
        private final ArrayList<Integer> list;
        private final int level;

        private EnchantedBookFindResult(MerchantEntity merchant, ArrayList<Integer> list, int level) {
            this.merchant = merchant;
            this.list = list;
            this.level = level;
        }

        @Override
        public Text get() {
            String key = "carpet.commands.finder.trade.enchanted_book.each";
            Text pos = TextProvider.blockPos(this.villagerPos(), Formatting.GREEN);
            // 村民或流浪商人的名称
            Text villagerName = merchant.getName();
            String indices = getIndexArray(this.list);
            // 获取交易名称
            Text enchantName = predicate.getWithLevel(level);
            return TextBuilder.translate(key, pos, villagerName, indices, enchantName);
        }

        @Override
        public BlockPos villagerPos() {
            return this.merchant.getBlockPos();
        }

        @Override
        public int compare(Result o1, Result o2) {
            EnchantedBookFindResult result1 = (EnchantedBookFindResult) o1;
            EnchantedBookFindResult result2 = (EnchantedBookFindResult) o2;
            int compare = Integer.compare(result1.level, result2.level);
            if (compare == 0) {
                return MathUtils.compareBlockPos(sourcePos, result1.villagerPos(), result2.villagerPos());
            }
            return -compare;
        }
    }
}
