package org.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.periodic.task.search.*;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.BlockEntityIterator;
import org.carpetorgaddition.wheel.BlockIterator;
import org.carpetorgaddition.wheel.ItemStackPredicate;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.permission.PermissionLevel;
import org.carpetorgaddition.wheel.permission.PermissionManager;
import org.carpetorgaddition.wheel.provider.TextProvider;

import java.io.File;
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
     * 村民的游戏内名称
     */
    public static final Text VILLAGER = TextBuilder.translate("entity.minecraft.villager");
    /**
     * 查找超时时抛出异常的反馈消息
     */
    public static final String TIME_OUT = "carpet.commands.finder.timeout";

    public FinderCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandFinder))
                .then(CommandManager.literal("block")
                        .requires(PermissionManager.register("finder.block", PermissionLevel.PASS))
                        .then(CommandManager.argument("blockState", BlockStateArgumentType.blockState(this.access))
                                .executes(context -> blockFinder(context, 64))
                                .then(CommandManager.argument("range", IntegerArgumentType.integer(0, 256))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> blockFinder(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(CommandManager.literal("from")
                                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.literal("to")
                                                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                                                .executes(this::areaBlockSearch)))))))
                .then(CommandManager.literal("item")
                        .requires(PermissionManager.register("finder.item", PermissionLevel.PASS))
                        .then(CommandManager.argument("itemStack", ItemPredicateArgumentType.itemPredicate(this.access))
                                .executes(context -> searchItem(context, 64))
                                .then(CommandManager.argument("range", IntegerArgumentType.integer(0, 256))
                                        .suggests(suggestionDefaultDistance())
                                        .executes(context -> searchItem(context, IntegerArgumentType.getInteger(context, "range"))))
                                .then(CommandManager.literal("from")
                                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.literal("to")
                                                        .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                                                .executes(this::areaItemFinder))))
                                        .then(CommandManager.literal("offline_player")
                                                .requires(PermissionManager.register("finder.item.from.offline_player", PermissionLevel.PASS))
                                                .executes(this::searchItemFromOfflinePlayer)))))
                .then(CommandManager.literal("trade")
                        .requires(PermissionManager.register("finder.trade", PermissionLevel.PASS))
                        .then(CommandManager.literal("item")
                                .then(CommandManager.argument("itemStack", ItemPredicateArgumentType.itemPredicate(this.access))
                                        .executes(context -> searchTradeItem(context, 64))
                                        .then(CommandManager.argument("range", IntegerArgumentType.integer(0, 256))
                                                .suggests(suggestionDefaultDistance())
                                                .executes(context -> searchTradeItem(context, IntegerArgumentType.getInteger(context, "range"))))))
                        .then(CommandManager.literal("enchanted_book")
                                .then(CommandManager.argument("enchantment", RegistryEntryReferenceArgumentType.registryEntry(this.access, RegistryKeys.ENCHANTMENT))
                                        .executes(context -> searchEnchantedBookTrade(context, 64))
                                        .then(CommandManager.argument("range", IntegerArgumentType.integer(0, 256))
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

    // 物品查找
    private int searchItem(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 获取玩家所在的位置，这是命令开始执行的坐标
        BlockPos sourceBlockPos = player.getBlockPos();
        // 查找周围容器中的物品
        World world = FetcherUtils.getWorld(player);
        ItemSearchTask task = new ItemSearchTask(world, predicate, new BlockEntityIterator(world, sourceBlockPos, range), context);
        ServerComponentCoordinator.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 区域查找物品
    private int areaItemFinder(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 获取要查找的物品
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 计算要查找的区域
        World world = FetcherUtils.getWorld(player);
        BlockEntityIterator blockEntityIterator = new BlockEntityIterator(world, from, to);
        ItemSearchTask task = new ItemSearchTask(world, predicate, blockEntityIterator, context);
        ServerComponentCoordinator.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 从离线玩家身上查找物品
    private int searchItemFromOfflinePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        MinecraftServer server = FetcherUtils.getServer(player);
        File[] files = server.getSavePath(WorldSavePath.PLAYERDATA).toFile().listFiles();
        if (files == null) {
            throw CommandUtils.createException("carpet.commands.finder.item.offline_player.unable_read_files");
        }
        UserCache userCache = server.getUserCache();
        if (userCache == null) {
            throw CommandUtils.createException("carpet.commands.finder.item.offline_player.unable_read_usercache");
        }
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        OfflinePlayerItemSearchContext argument = new OfflinePlayerItemSearchContext(context.getSource(), predicate, userCache, player, files);
        ServerTask task = new OfflinePlayerSearchTask(argument);
        ServerComponentCoordinator.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 方块查找
    private int blockFinder(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取要匹配的方块状态
        BlockStateArgument argument = BlockStateArgumentType.getBlockState(context, "blockState");
        // 获取命令执行时的方块坐标
        final BlockPos sourceBlockPos = player.getBlockPos();
        ServerWorld world = FetcherUtils.getWorld(player);
        BlockIterator blockIterator = new BlockIterator(world, sourceBlockPos, range);
        ArgumentBlockPredicate predicate = new ArgumentBlockPredicate(argument);
        BlockSearchTask task = new BlockSearchTask(world, sourceBlockPos, blockIterator, context, predicate);
        ServerComponentCoordinator.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 查找可能影响世吞运行的方块
    private int mayAffectWorldEater(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        // 获取执行命令的玩家并非空判断
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 获取命令执行时的方块坐标
        final BlockPos sourceBlockPos = player.getBlockPos();
        ServerWorld world = FetcherUtils.getWorld(player);
        BlockIterator blockIterator = new BlockIterator(from, to);
        BlockBlockPredicate predicate = new BlockBlockPredicate();
        MayAffectWorldEaterBlockSearchTask task = new MayAffectWorldEaterBlockSearchTask(world, sourceBlockPos, blockIterator, context, predicate);
        ServerComponentCoordinator.getManager(context).getServerTaskManager().addTask(task);
        return 0;
    }

    // 区域方块查找
    private int areaBlockSearch(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
        BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
        // 获取要匹配的方块状态
        BlockStateArgument argument = BlockStateArgumentType.getBlockState(context, "blockState");
        // 计算要查找的区域
        BlockIterator blockIterator = new BlockIterator(from, to);
        ArgumentBlockPredicate predicate = new ArgumentBlockPredicate(argument);
        // 添加查找任务
        BlockSearchTask task = new BlockSearchTask(FetcherUtils.getWorld(player), player.getBlockPos(), blockIterator, context, predicate);
        ServerComponentCoordinator.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 准备根据物品查找交易项
    private int searchTradeItem(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家对象
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取要匹配的物品
        ItemStackPredicate predicate = new ItemStackPredicate(context, "itemStack");
        // 获取玩家所在的坐标
        BlockPos sourcePos = player.getBlockPos();
        World world = FetcherUtils.getWorld(player);
        // 查找范围
        BlockIterator area = new BlockIterator(world, sourcePos, range);
        TradeItemSearchTask task = new TradeItemSearchTask(world, area, sourcePos, predicate, context);
        // 向任务管理器添加任务
        ServerComponentCoordinator.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 准备查找出售指定附魔书的村民
    private int searchEnchantedBookTrade(CommandContext<ServerCommandSource> context, int range) throws CommandSyntaxException {
        // 获取执行命令的玩家
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取需要查找的附魔
        Enchantment enchantment = RegistryEntryReferenceArgumentType.getEnchantment(context, "enchantment").value();
        // 获取玩家所在的位置
        BlockPos sourcePos = player.getBlockPos();
        World world = FetcherUtils.getWorld(player);
        // 查找范围
        BlockIterator area = new BlockIterator(world, sourcePos, range);
        TradeEnchantedBookSearchTask task = new TradeEnchantedBookSearchTask(world, area, sourcePos, context, enchantment);
        // 向任务管理器添加任务
        ServerComponentCoordinator.getManager(context).getServerTaskManager().addTask(task);
        return 1;
    }

    // 将物品数量转换为“多少组多少个”的形式
    public static Text showCount(ItemStack itemStack, int count, boolean inTheShulkerBox) {
        TextBuilder builder = new TextBuilder(TextProvider.itemCount(count, itemStack.getMaxCount()));
        // 如果包含在潜影盒内找到的物品，在数量上添加斜体效果
        return inTheShulkerBox ? builder.setItalic().build() : builder.build();
    }

    @Override
    public String getDefaultName() {
        return "finder";
    }

    public interface BlockPredicate {
        boolean test(ServerWorld world, BlockPos pos);

        Text getName();
    }

    public record ArgumentBlockPredicate(BlockStateArgument argument) implements BlockPredicate {
        @Override
        public boolean test(ServerWorld world, BlockPos pos) {
            return argument.test(world, pos);
        }

        @Override
        public Text getName() {
            return argument.getBlockState().getBlock().getName();
        }
    }

    public static class BlockBlockPredicate implements BlockPredicate {
        @Override
        public boolean test(ServerWorld world, BlockPos pos) {
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

        private boolean canPush(ServerWorld world, BlockPos pos) {
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
        public Text getName() {
            return TextBuilder.translate("carpet.commands.finder.may_affect_world_eater_block.name");
        }
    }
}
