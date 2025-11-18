package org.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockPredicateArgumentType;
import net.minecraft.command.argument.ItemPredicateArgumentType;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.periodic.task.search.*;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.permission.PermissionLevel;
import org.carpetorgaddition.wheel.permission.PermissionManager;
import org.carpetorgaddition.wheel.predicate.BlockStatePredicate;
import org.carpetorgaddition.wheel.predicate.EnchantedBookPredicate;
import org.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.carpetorgaddition.wheel.traverser.BlockEntityTraverser;
import org.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import org.carpetorgaddition.wheel.traverser.WorldTraverser;

import java.util.function.Predicate;

public class FinderCommand extends AbstractServerCommand {
    /**
     * 最大统计数量
     */
    public static final int MAXIMUM_STATISTICAL_COUNT = 30000;
    /**
     * 每个游戏刻最大查找时间
     */
    public static final long MAX_FIND_TIME = 200;
    /**
     * 任务执行的最大游戏刻数
     */
    public static final int MAX_TICK_COUNT = 50;
    /**
     * 最大查找半径
     */
    public static final int MAX_HORIZONTAL_RANGE = 256;
    /**
     * 村民的游戏内名称
     */
    public static final Text VILLAGER = TextBuilder.translate("entity.minecraft.villager");
    /**
     * 查找超时时抛出异常的反馈消息
     */
    public static final String TIME_OUT = "carpet.commands.finder.timeout";
    public static final String FINDER_BLOCK = "finder.block";
    public static final String FINDER_ITEM = "finder.item";
    public static final String FINDER_ITEM_FROM_OFFLINE_PLAYER = "finder.item.from.offline_player";

    public FinderCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandFinder))
                .then(CommandManager.literal("block")
                        .requires(PermissionManager.register(FINDER_BLOCK, PermissionLevel.PASS))
                        .then(CommandManager.argument("blockState", BlockPredicateArgumentType.blockPredicate(this.access))
                                .executes(context -> blockFinder(context, 64))
                                .then(CommandManager.argument("range", IntegerArgumentType.integer(0, MAX_HORIZONTAL_RANGE))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> blockFinder(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(CommandManager.literal("from")
                                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.literal("to")
                                                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                                                .executes(this::areaBlockSearch)))))))
                .then(CommandManager.literal("item")
                        .requires(PermissionManager.register(FINDER_ITEM, PermissionLevel.PASS))
                        .then(CommandManager.argument("itemStack", ItemPredicateArgumentType.itemPredicate(this.access))
                                .executes(context -> searchItem(context, 64))
                                .then(CommandManager.argument("range", IntegerArgumentType.integer(0, MAX_HORIZONTAL_RANGE))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> searchItem(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(CommandManager.literal("from")
                                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.literal("to")
                                                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                                                .executes(this::areaItemFinder))))
                                        .then(CommandManager.literal("offline_player")
                                                .requires(PermissionManager.register(FINDER_ITEM_FROM_OFFLINE_PLAYER, PermissionLevel.PASS))
                                                .executes(this::searchItemFromOfflinePlayer)))))
                .then(CommandManager.literal("trade")
                        .requires(PermissionManager.register("finder.trade", PermissionLevel.PASS))
                        .then(CommandManager.literal("item")
                                .then(CommandManager.argument("itemStack", ItemPredicateArgumentType.itemPredicate(this.access))
                                        .executes(context -> searchTradeItem(context, 64))
                                        .then(CommandManager.argument("range", IntegerArgumentType.integer(0, MAX_HORIZONTAL_RANGE))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> searchTradeItem(context, IntegerArgumentType.getInteger(context, "range"))))))
                        .then(CommandManager.literal("enchanted_book")
                                .then(CommandManager.argument("enchantment", RegistryEntryReferenceArgumentType.registryEntry(this.access, RegistryKeys.ENCHANTMENT))
                                        .executes(context -> searchEnchantedBookTrade(context, 64))
                                        .then(CommandManager.argument("range", IntegerArgumentType.integer(0, MAX_HORIZONTAL_RANGE))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> searchEnchantedBookTrade(context, IntegerArgumentType.getInteger(context, "range")))))))
                .then(CommandManager.literal("worldEater")
                        .requires(((Predicate<ServerCommandSource>) source -> CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION)
                                .and(PermissionManager.registerHiddenCommand("finder.worldEater", PermissionLevel.PASS)))
                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                        .executes(this::mayAffectWorldEater)))));
    }

    private SuggestionProvider<ServerCommandSource> suggestionDefaultDistance() {
        return (context, builder) -> CommandSource.suggestMatching(new String[]{"64", "128", "256"}, builder);
    }

    /**
     * 物品查找
     */
    private int searchItem(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 获取玩家所在的位置，这是命令开始执行的坐标
        BlockPos sourceBlockPos = player.getBlockPos();
        // 查找周围容器中的物品
        World world = FetcherUtils.getWorld(player);
        BlockEntityTraverser traverser = new BlockEntityTraverser(world, sourceBlockPos, range);
        this.checkBoxSize(traverser);
        ItemSearchTask task = new ItemSearchTask(world, predicate, traverser, context.getSource());
        ServerComponentCoordinator.getCoordinator(context).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 区域查找物品
     */
    private int areaItemFinder(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 获取要查找的物品
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 计算要查找的区域
        World world = FetcherUtils.getWorld(player);
        BlockEntityTraverser traverser = new BlockEntityTraverser(world, from, to);
        this.checkBoxSize(traverser);
        ItemSearchTask task = new ItemSearchTask(world, predicate, traverser, context.getSource());
        ServerComponentCoordinator.getCoordinator(context).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 从离线玩家身上查找物品
     */
    private int searchItemFromOfflinePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        ServerTask task = new OfflinePlayerSearchTask(context.getSource(), predicate, player);
        ServerComponentCoordinator.getCoordinator(context).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 方块查找
     */
    private int blockFinder(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取命令执行时的方块坐标
        final BlockPos sourceBlockPos = player.getBlockPos();
        ServerWorld world = FetcherUtils.getWorld(player);
        BlockPosTraverser traverser = new BlockPosTraverser(world, sourceBlockPos, range);
        this.checkBoxSize(traverser);
        BlockStatePredicate predicate = BlockStatePredicate.ofPredicate(context, "blockState");
        BlockSearchTask task = new BlockSearchTask(world, sourceBlockPos, traverser, context.getSource(), predicate);
        ServerComponentCoordinator.getCoordinator(context).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 查找可能影响世吞运行的方块
     */
    private int mayAffectWorldEater(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 获取命令执行时的方块坐标
        final BlockPos sourceBlockPos = player.getBlockPos();
        ServerWorld world = FetcherUtils.getWorld(player);
        BlockPosTraverser traverser = new BlockPosTraverser(from, to);
        this.checkBoxSize(traverser);
        BlockStatePredicate predicate = BlockStatePredicate.ofWorldEater();
        BlockSearchTask task = new BlockSearchTask(world, sourceBlockPos, traverser, context.getSource(), predicate);
        ServerComponentCoordinator.getCoordinator(context).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 区域方块查找
     */
    private int areaBlockSearch(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 计算要查找的区域
        BlockPosTraverser traverser = new BlockPosTraverser(from, to);
        this.checkBoxSize(traverser);
        BlockStatePredicate predicate = BlockStatePredicate.ofPredicate(context, "blockState");
        // 添加查找任务
        BlockSearchTask task = new BlockSearchTask(FetcherUtils.getWorld(player), player.getBlockPos(), traverser, context.getSource(), predicate);
        ServerComponentCoordinator.getCoordinator(context).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 准备根据物品查找交易项
     */
    private int searchTradeItem(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家对象
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取要匹配的物品
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 获取玩家所在的坐标
        BlockPos sourcePos = player.getBlockPos();
        World world = FetcherUtils.getWorld(player);
        // 查找范围
        BlockPosTraverser traverser = new BlockPosTraverser(world, sourcePos, range);
        this.checkBoxSize(traverser);
        TradeItemSearchTask task = new TradeItemSearchTask(world, traverser, sourcePos, predicate, context.getSource());
        // 向任务管理器添加任务
        ServerComponentCoordinator.getCoordinator(context).getServerTaskManager().addTask(task);
        return 1;
    }

    /**
     * 准备查找出售指定附魔书的村民
     */
    private int searchEnchantedBookTrade(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取需要查找的附魔
        Enchantment enchantment = RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment").value();
        ServerCommandSource source = context.getSource();
        MinecraftServer server = source.getServer();
        EnchantedBookPredicate predicate = new EnchantedBookPredicate(server, enchantment);
        // 获取玩家所在的位置
        BlockPos sourcePos = player.getBlockPos();
        World world = FetcherUtils.getWorld(player);
        // 查找范围
        BlockPosTraverser traverser = new BlockPosTraverser(world, sourcePos, range);
        this.checkBoxSize(traverser);
        TradeEnchantedBookSearchTask task = new TradeEnchantedBookSearchTask(world, traverser, sourcePos, source, predicate);
        // 向任务管理器添加任务
        ServerComponentCoordinator.getCoordinator(context).getServerTaskManager().addTask(task);
        return 1;
    }

    private void checkBoxSize(WorldTraverser<?> traverser) throws CommandSyntaxException {
        int max = (MAX_HORIZONTAL_RANGE << 1) + 1;
        if (traverser.length() > max || traverser.width() > max) {
            throw CommandUtils.createException("carpet.commands.finder.toobig", max);
        }
    }

    /**
     * 将物品数量转换为“多少组多少个”的形式
     */
    public static Text showCount(ItemStack itemStack, int count, boolean inTheShulkerBox) {
        TextBuilder builder = new TextBuilder(TextProvider.itemCount(count, itemStack.getMaxCount()));
        // 如果包含在潜影盒内找到的物品，在数量上添加斜体效果
        return inTheShulkerBox ? builder.setItalic().build() : builder.build();
    }

    @Override
    public String getDefaultName() {
        return "finder";
    }
}
