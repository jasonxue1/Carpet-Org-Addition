package org.carpetorgaddition.periodic.task.search;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.mixin.accessor.AbstractHorseEntityAccessor;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.BlockEntityIterator;
import org.carpetorgaddition.wheel.ItemStackPredicate;
import org.carpetorgaddition.wheel.ItemStackStatistics;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.inventory.ImmutableInventory;
import org.carpetorgaddition.wheel.page.PageManager;
import org.carpetorgaddition.wheel.page.PagedCollection;
import org.carpetorgaddition.wheel.provider.TextProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class ItemSearchTask extends ServerTask {
    private final World world;
    private final BlockEntityIterator blockEntities;
    private final CommandContext<ServerCommandSource> context;
    private Iterator<Entity> entitySearchIterator;
    private Iterator<BlockEntity> blockEntitySearchIterator;
    private FindState findState;
    private int count;
    private boolean shulkerBox;
    /**
     * tick方法开始执行时的时间
     */
    private long startTime;
    /**
     * 任务被执行的总游戏刻数
     */
    private int tickCount;
    private final ItemStackPredicate predicate;
    private final ArrayList<Result> results = new ArrayList<>();
    private final PagedCollection pagedCollection;

    public ItemSearchTask(World world, ItemStackPredicate predicate, BlockEntityIterator blockEntities, CommandContext<ServerCommandSource> context) {
        this.world = world;
        this.blockEntities = blockEntities;
        this.findState = FindState.BLOCK;
        this.tickCount = 0;
        this.predicate = predicate;
        this.context = context;
        PageManager pageManager = FetcherUtils.getPageManager(context.getSource().getServer());
        this.pagedCollection = pageManager.newPagedCollection(this.context.getSource());
    }

    @Override
    public void tick() {
        this.startTime = System.currentTimeMillis();
        this.tickCount++;
        if (tickCount > FinderCommand.MAX_TICK_COUNT) {
            // 任务超时
            MessageUtils.sendErrorMessage(context, FinderCommand.TIME_OUT);
            this.findState = FindState.END;
            return;
        }
        while (true) {
            if (timeout()) {
                return;
            }
            try {
                switch (this.findState) {
                    case BLOCK -> searchFromContainer();
                    case ENTITY -> searchFromEntity();
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

    // 从容器查找
    private void searchFromContainer() {
        if (this.blockEntitySearchIterator == null) {
            this.blockEntitySearchIterator = blockEntities.iterator();
        }
        while (this.blockEntitySearchIterator.hasNext()) {
            if (this.timeout()) {
                return;
            }
            BlockEntity blockEntity = this.blockEntitySearchIterator.next();
            BlockPos blockPos = blockEntity.getPos();
            if (blockEntity instanceof Inventory inventory) {
                // 获取容器名称
                Text containerName
                        = inventory instanceof LockableContainerBlockEntity lockableContainer
                        ? lockableContainer.getName()
                        : this.world.getBlockState(blockPos).getBlock().getName();
                this.count(inventory, blockPos, containerName);
            }
        }
        this.findState = FindState.ENTITY;
    }

    // 从实体查找
    private void searchFromEntity() {
        Box box = blockEntities.toBox();
        if (this.entitySearchIterator == null) {
            this.entitySearchIterator = this.world.getNonSpectatingEntities(Entity.class, box).iterator();
        }
        while (this.entitySearchIterator.hasNext()) {
            if (timeout()) {
                return;
            }
            Entity entity = this.entitySearchIterator.next();
            switch (entity) {
                // 掉落物
                case ItemEntity itemEntity -> {
                    Text drops = TextBuilder.translate("carpet.commands.finder.item.drops");
                    this.count(new SimpleInventory(itemEntity.getStack()), itemEntity.getBlockPos(), drops);
                }
                // 假玩家
                case EntityPlayerMPFake fakePlayer -> {
                    PlayerInventory inventory = fakePlayer.getInventory();
                    this.count(inventory, fakePlayer.getBlockPos(), fakePlayer.getName());
                }
                // 容器实体
                case VehicleInventory inventory -> this.count(inventory, entity.getBlockPos(), entity.getName());
                // 物品展示框
                case ItemFrameEntity itemFrame -> {
                    ItemStack itemStack = itemFrame.getHeldItemStack();
                    if (itemStack != null) {
                        ImmutableInventory inventory = new ImmutableInventory(itemStack);
                        this.count(inventory, itemFrame.getBlockPos(), itemFrame.getName());
                    }
                }
                // 村民
                case VillagerEntity villager when CarpetOrgAdditionSettings.openVillagerInventory.get() -> {
                    SimpleInventory inventory = villager.getInventory();
                    this.count(inventory, villager.getBlockPos(), villager.getName());
                }
                // 驴，骡和羊驼
                case AbstractDonkeyEntity donkey when donkey.hasChest() -> {
                    ArrayList<ItemStack> list = new ArrayList<>();
                    AbstractHorseEntityAccessor accessor = (AbstractHorseEntityAccessor) donkey;
                    SimpleInventory inventory = accessor.getDonkeyInventory();
                    // 跳过第一个物品，第一个物品是鞍
                    for (int i = 1; i < inventory.size(); i++) {
                        list.add(inventory.getStack(i));
                    }
                    this.count(new ImmutableInventory(list), donkey.getBlockPos(), donkey.getName());
                }
                // 悦灵
                case AllayEntity allay -> {
                    ImmutableInventory inventory = new ImmutableInventory(allay.getMainHandStack());
                    this.count(inventory, allay.getBlockPos(), allay.getName());
                }
                default -> {
                }
            }
        }
        this.findState = FindState.SORT;
    }

    // 统计符合条件的物品
    private void count(Inventory inventory, BlockPos blockPos, Text containerName) {
        // 是否有物品是在潜影盒中找到的
        ItemStackStatistics statistics = new ItemStackStatistics(this.predicate);
        statistics.statistics(inventory);
        if (statistics.getSum() == 0) {
            return;
        }
        this.count += statistics.getSum();
        if (statistics.hasNestingItem()) {
            this.shulkerBox = true;
        }
        this.results.add(new Result(blockPos, containerName, statistics));
        if (this.results.size() > FinderCommand.MAXIMUM_STATISTICAL_COUNT) {
            // 容器太多，无法统计
            Runnable function = () -> MessageUtils.sendErrorMessage(
                    context,
                    "carpet.commands.finder.item.too_much_container",
                    this.predicate.toText()
            );
            throw new TaskExecutionException(function);
        }
    }

    // 对结果进行排序
    private void sort() {
        if (this.results.isEmpty()) {
            // 在周围的容器中找不到指定物品，直接将状态设置为结束，然后结束方法
            MessageUtils.sendMessage(context.getSource(), "carpet.commands.finder.item.find.not_item", predicate.toText());
            this.findState = FindState.END;
            return;
        }
        this.results.sort((o1, o2) -> o2.statistics().getSum() - o1.statistics().getSum());
        this.findState = FindState.FEEDBACK;
    }

    // 发送命令反馈
    private void feedback() {
        Text itemCount;
        Optional<Item> optional = predicate.getConvert();
        if (optional.isPresent()) {
            // 为数量添加鼠标悬停效果
            itemCount = FinderCommand.showCount(optional.get().getDefaultStack(), this.count, this.shulkerBox);
        } else {
            TextBuilder builder = new TextBuilder(this.count);
            if (this.shulkerBox) {
                builder.setItalic();
            }
            itemCount = builder.build();
        }
        int size = this.results.size();
        MessageUtils.sendEmptyMessage(this.context);
        MessageUtils.sendMessage(context.getSource(), "carpet.commands.finder.item.find", size, itemCount, predicate.toText());
        this.pagedCollection.addContent(this.results);
        CommandUtils.handlingException(this.pagedCollection::print, this.context);
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

    @Override
    public int hashCode() {
        return Objects.hashCode(context.getSource().getPlayer());
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            return Objects.equals(this.context.getSource().getPlayer(), ((ItemSearchTask) obj).context.getSource().getPlayer());
        }
        return false;
    }

    public record Result(
            BlockPos blockPos,
            Text containerName,
            ItemStackStatistics statistics
    ) implements Supplier<Text> {
        @Override
        public Text get() {
            return TextBuilder.translate(
                    "carpet.commands.finder.item.each",
                    TextProvider.blockPos(blockPos, Formatting.GREEN),
                    containerName,
                    statistics.getCountText()
            );
        }
    }

    public enum FindState {
        BLOCK,
        ENTITY,
        SORT,
        FEEDBACK,
        END
    }

    @Override
    public String getLogName() {
        return "物品查找";
    }
}
