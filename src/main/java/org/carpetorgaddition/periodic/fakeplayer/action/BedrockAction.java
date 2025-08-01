package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.MutableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableInt;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerPathfinder;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.periodic.fakeplayer.action.bedrock.*;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.wheel.BlockEntityIterator;
import org.carpetorgaddition.wheel.BlockIterator;
import org.carpetorgaddition.wheel.Counter;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BedrockAction extends AbstractPlayerAction {
    private final LinkedHashSet<BedrockBreakingContext> contexts = new LinkedHashSet<>();
    private final HashSet<BlockPos> lavas = new HashSet<>();
    private final BlockIterator blockIterator;
    private final BedrockRegionType regionType;
    @NotNull
    private FakePlayerPathfinder pathfinder = FakePlayerPathfinder.EMPTY;
    /**
     * 玩家是否有AI，是否可以自动寻路，自动进食
     */
    private final boolean ai;
    /**
     * 下一次定时回收材料的剩余游戏刻数，剩余时间为-1表示禁用定时回收
     */
    private int recycleTimer;
    @Nullable
    private BedrockBreakingContext currentContext;
    @NotNull
    private PlayerWorkPhase phase = PlayerWorkPhase.WORK;
    /**
     * 玩家上一个任务阶段
     */
    @NotNull
    private PlayerWorkPhase prevPhase = getPhase();
    /**
     * 玩家当前的移动目标
     */
    @Nullable
    private BlockPos bedrockTarget;
    /**
     * 距离最近的物品实体
     */
    @Nullable
    private ItemEntity recentItemEntity;
    /**
     * 正在移动到近处的基岩
     */
    private boolean isMovingToNearbyBedrock = false;
    /**
     * 当前游戏刻玩家是否执行了破基岩的动作，例如放置活塞，点击拉杆等
     */
    private boolean hasAction;
    /**
     * 是否刚刚完成材料收集，用于在开始破基岩时清除之前收集材料的寻路
     */
    private boolean materialCollectionComplete = false;
    /**
     * 因处于特殊位置而无法破除的基岩
     */
    private final Counter<BlockPos> invalidBedrock = new Counter<>();
    /**
     * 周围的材料物品
     */
    private final ArrayList<ItemEntity> itemEntities = new ArrayList<>();
    /**
     * 定时回收材料的时间间隔（3分钟）
     */
    private static final int MATERIAL_RECYCLING_TIME = 3600;

    private BedrockAction(EntityPlayerMPFake fakePlayer, BlockIterator blockIterator, BedrockRegionType regionType, boolean ai, boolean timedMaterialRecycling) {
        super(fakePlayer);
        this.blockIterator = blockIterator;
        this.regionType = regionType;
        this.ai = ai;
        this.recycleTimer = timedMaterialRecycling ? MATERIAL_RECYCLING_TIME : -1;
    }

    public BedrockAction(EntityPlayerMPFake fakePlayer, BlockPos from, BlockPos to, boolean ai, boolean timedMaterialRecycling) {
        this(fakePlayer, new BlockIterator(from, to), BedrockRegionType.CUBOID, ai, timedMaterialRecycling);
    }

    public BedrockAction(EntityPlayerMPFake fakePlayer, BlockPos center, int radius, int height, boolean ai, boolean timedMaterialRecycling) {
        this(fakePlayer, new CylinderBlockIterator(center, radius, height), BedrockRegionType.CYLINDER, ai, timedMaterialRecycling);
    }

    @Override
    protected void tick() {
        if (this.ai) {
            this.pathfinder.tick();
            EntityPlayerMPFake fakePlayer = this.getFakePlayer();
            // 周围有下落的方块，暂停寻路，防止被砸死
            if (BlockEntityIterator.ofAbove(fakePlayer, 3).contains(FallingBlockEntity.class)) {
                this.pathfinder.pause(3);
            }
            this.itemEntities.removeIf(Entity::isRemoved);
            if (this.recentItemEntity != null && this.recentItemEntity.isRemoved()) {
                this.recentItemEntity = null;
            }
            if (shouldEat()) {
                if (this.getPhase() != PlayerWorkPhase.EAT) {
                    this.prevPhase = this.getPhase();
                }
                this.setPhase(PlayerWorkPhase.EAT);
            } else if (this.recycleTimer > 0) {
                this.recycleTimer--;
                if (this.recycleTimer == 0) {
                    this.setPhase(PlayerWorkPhase.COLLECT);
                }
            }
            if (this.recycleTimer < -1) {
                throw new IllegalStateException("The remaining time for material recycling should not be %s".formatted(this.recycleTimer));
            }
            switch (this.getPhase()) {
                case WORK -> work();
                case EAT -> eat();
                case COLLECT -> collectingMaterials();
            }
        } else {
            this.work();
        }
    }

    private void work() {
        this.invalidBedrock.trim();
        this.hasAction = false;
        if (this.materialCollectionComplete) {
            this.materialCollectionComplete = false;
            this.bedrockTarget = null;
        }
        this.drainFluidLava();
        for (BlockPos blockPos : this.invalidBedrock) {
            this.invalidBedrock.decrement(blockPos);
        }
        if (this.tickCurrentWork()) {
            return;
        }
        World world = this.getFakePlayer().getWorld();
        this.removeIf(context -> {
            if (context.getState() == BreakingState.COMPLETE) {
                return true;
            }
            if (world.getBlockState(context.getBedrockPos()).isOf(Blocks.BEDROCK)) {
                return !canInteract(context.getBedrockPos());
            }
            return context.getState() != BreakingState.CLEAN_PISTON;
        });
        double range = this.getFakePlayer().getBlockInteractionRange();
        // 如果this.selectionArea过大，遍历时可能会造成大量卡顿
        Box box = new Box(this.getFakePlayer().getBlockPos()).expand(Math.min(range, 10.0));
        BlockIterator area = new BlockIterator(box);
        for (BlockPos blockPos : area) {
            if (canInteract(blockPos) && this.inSelectionArea(blockPos)) {
                BlockState blockState = world.getBlockState(blockPos);
                if (blockState.isOf(Blocks.BEDROCK)) {
                    this.add(new BedrockBreakingContext(blockPos));
                } else if (blockState.isOf(Blocks.LAVA)) {
                    this.lavas.add(blockPos);
                }
            }
        }
        for (BedrockBreakingContext context : this.contexts) {
            if (context == this.currentContext) {
                continue;
            }
            if (tickWork(context)) {
                return;
            }
        }
        if (this.ai) {
            this.setGotoTarget(world);
        }
    }

    /**
     * 设置目标基岩位置
     */
    private void setGotoTarget(World world) {
        if (this.hasAction) {
            if (this.pathfinder.isFinished()) {
                this.bedrockTarget = null;
            }
            return;
        }
        this.isMovingToNearbyBedrock = false;
        Optional<BlockPos> optional = this.getMovingTarget();
        if (optional.isPresent() && this.canInteract(optional.get())) {
            // 基岩在交互距离内，但因位置特殊而无法破除的基岩
            // 重新选择目标位置，并将当前目标位置标记为无效位置
            this.selectRandomBedrock(world);
            this.invalidBedrock.set(optional.get(), 200);
            return;
        }
        for (BedrockBreakingContext context : this.contexts) {
            BlockPos blockPos = context.getBedrockPos();
            if (this.invalidBedrock.getCount(blockPos) > 0) {
                continue;
            }
            this.bedrockTarget = blockPos;
            return;
        }
        this.selectRandomBedrock(world);
    }

    /**
     * 随机选择基岩位置
     */
    private void selectRandomBedrock(World world) {
        if (this.pathfinder.isInvalid()) {
            for (int i = 0; i < 100; i++) {
                BlockPos blockPos = this.blockIterator.randomBlockPos();
                if (world.getBlockState(blockPos).isOf(Blocks.BEDROCK)) {
                    this.bedrockTarget = blockPos;
                    return;
                }
            }
        }
    }

    /**
     * @return 指定方块坐标是否在玩家交互距离内
     */
    private boolean canInteract(BlockPos blockPos) {
        return this.getFakePlayer().canInteractWithBlockAt(blockPos, 0.0);
    }

    private boolean tickCurrentWork() {
        if (this.currentContext == null) {
            return false;
        }
        if (this.contexts.contains(this.currentContext)) {
            if (!this.isMovingToNearbyBedrock) {
                this.isMovingToNearbyBedrock = true;
                this.bedrockTarget = this.currentContext.getBedrockPos();
            }
            return this.tickWork(this.currentContext);
        }
        this.currentContext = null;
        return false;
    }

    private boolean tickWork(BedrockBreakingContext context) {
        int loopCount = 0;
        loop:
        while (true) {
            loopCount++;
            if (loopCount > 10) {
                throw new InfiniteLoopException();
            }
            StepResult stepResult = start(context);
            switch (stepResult) {
                case COMPLETION -> {
                    break loop;
                }
                case TICK_COMPLETION -> {
                    this.currentContext = context;
                    return true;
                }
                default -> {
                }
            }
        }
        return false;
    }

    private StepResult start(BedrockBreakingContext context) {
        BlockPos bedrockPos = context.getBedrockPos();
        switch (context.getState()) {
            case PLACE_THE_PISTON_FACING_UP -> {
                if (hasMaterial()) {
                    StepResult stepResult = placePiston(bedrockPos);
                    if (stepResult == StepResult.CONTINUE) {
                        context.nextStep();
                    } else {
                        return stepResult;
                    }
                } else {
                    // 玩家没有足够的材料
                    this.setPhase(PlayerWorkPhase.COLLECT);
                    return StepResult.TICK_COMPLETION;
                }
            }
            case PLACE_AND_ACTIVATE_THE_LEVER -> {
                StepResult stepResult = placeAndActivateTheLever(context);
                if (stepResult == StepResult.CONTINUE) {
                    context.nextStep();
                } else {
                    return stepResult;
                }
                // 不管是否成功放置都结束方法
                return StepResult.COMPLETION;
            }
            case PISTON_BREAK_BEDROCK -> {
                StepResult stepResult = pistonBreakBedrock(context);
                switch (stepResult) {
                    // 基岩破除，结束当前位置
                    case COMPLETION -> {
                        context.nextStep();
                        return StepResult.COMPLETION;
                    }
                    // 活塞没有挖掘完毕，结束当前tick
                    case TICK_COMPLETION -> {
                        return StepResult.TICK_COMPLETION;
                    }
                    case FAIL -> {
                        context.fail();
                        return StepResult.COMPLETION;
                    }
                    default -> throw new IllegalStateException();
                }
            }
            case CLEAN_PISTON -> {
                if (cleanPiston(bedrockPos.up())) {
                    // 活塞挖掘完毕，执行下一步
                    context.nextStep();
                } else {
                    return StepResult.TICK_COMPLETION;
                }
            }
            default -> {
                return StepResult.COMPLETION;
            }
        }
        return StepResult.CONTINUE;
    }

    /**
     * @return 玩家是否有足够的材料
     */
    private boolean hasMaterial() {
        MutableInt pistonCount = new MutableInt(0);
        MutableInt levelCount = new MutableInt(0);
        // 遍历物品栏和副手，不遍历盔甲槽
        ArrayList<ItemStack> list = new ArrayList<>(this.getFakePlayer().getInventory().getMainStacks());
        list.add(this.getFakePlayer().getOffHandStack());
        if (hasMaterial(list, pistonCount, levelCount)) {
            return true;
        }
        if (CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.get()) {
            return list.stream()
                    .filter(InventoryUtils::isOperableSulkerBox)
                    .map(ContainerComponentInventory::new)
                    .anyMatch(inventory -> hasMaterial(inventory, pistonCount, levelCount));
        }
        return false;
    }

    private static boolean hasMaterial(Iterable<ItemStack> list, MutableInt pistonCount, MutableInt levelCount) {
        for (ItemStack itemStack : list) {
            if (itemStack.isOf(Items.PISTON)) {
                pistonCount.add(itemStack.getCount());
            } else if (itemStack.isOf(Items.LEVER)) {
                levelCount.add(itemStack.getCount());
            }
            if (pistonCount.getValue() >= 2 && levelCount.getValue() >= 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在基岩上方放置一个朝上的活塞
     *
     * @return 是否放置成功
     */
    private StepResult placePiston(BlockPos bedrockPos) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        if (this.aboveHasFallingBlockEntity(fakePlayer.getWorld(), bedrockPos)) {
            return StepResult.COMPLETION;
        }
        World world = this.getFakePlayer().getWorld();
        BlockPos up = bedrockPos.up(1);
        BlockState blockState = world.getBlockState(up);
        BlockExcavator blockExcavator = FetcherUtils.getBlockExcavator(this.getFakePlayer());
        boolean isPiston = false;
        //noinspection StatementWithEmptyBody
        if (isReplaceableBlock(blockState)) {
            // 当前方块是可以被直接替换的，例如雪
            // 什么也不需要不做
        } else if (blockState.isOf(Blocks.PISTON) && blockState.get(PistonBlock.FACING) == Direction.UP) {
            // 当方块已经是活塞了，不需要再次放置
            isPiston = true;
        } else if (canMine(blockState, world, up)) {
            return tickBreakBlock(blockExcavator, up);
        } else {
            return StepResult.COMPLETION;
        }
        up = bedrockPos.up(2);
        blockState = world.getBlockState(up);
        if (isPiston && blockState.isOf(Blocks.PISTON_HEAD) && blockState.get(PistonHeadBlock.FACING) == Direction.UP) {
            // 上方方块是下方活塞伸出的活塞头
            return StepResult.CONTINUE;
        }
        // 活塞上方的方块不会影响活塞退出
        if (blockState.isAir() || blockState.getPistonBehavior() == PistonBehavior.DESTROY) {
            if (isPiston) {
                return StepResult.CONTINUE;
            }
            // 放置活塞
            if (placePiston(bedrockPos, Direction.UP).isAccepted()) {
                return StepResult.CONTINUE;
            }
            return StepResult.COMPLETION;
        } else if (canMine(blockState, world, up)) {
            return tickBreakBlock(blockExcavator, up);
        } else {
            return StepResult.COMPLETION;
        }
    }

    private boolean canMine(BlockState blockState, World world, BlockPos blockPos) {
        if (blockState.isAir()) {
            return true;
        }
        boolean isPiston = blockState.isOf(Blocks.PISTON);
        if (isPiston) {
            // 允许破坏方向不正确的活塞
            if (blockState.get(PistonBlock.FACING).getAxis() != Direction.Axis.Y) {
                return true;
            }
            // 允许破坏浮空的活塞
            if (!world.getBlockState(blockPos.down()).isOf(Blocks.BEDROCK)) {
                return true;
            }
            // 允许破坏放置在基岩上但朝下的活塞
            if (blockState.get(PistonBlock.FACING) == Direction.DOWN) {
                return true;
            }
        }
        // 允许破坏没有附着在方块侧面的拉杆
        if (blockState.isOf(Blocks.LEVER)) {
            return blockState.get(LeverBlock.FACE) != BlockFace.WALL;
        }
        if (isPiston || blockState.isOf(Blocks.PISTON_HEAD)) {
            return false;
        }
        return blockState.getHardness(world, blockPos) != -1 && BlockExcavator.canBreak(this.getFakePlayer(), blockPos);
    }

    /**
     * 在基岩上放置并激活拉杆
     *
     * @return 拉杆是否放置并激活成功
     */
    private StepResult placeAndActivateTheLever(BedrockBreakingContext context) {
        BlockPos bedrockPos = context.getBedrockPos();
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        World world = fakePlayer.getWorld();
        ServerPlayerInteractionManager interactionManager = fakePlayer.interactionManager;
        Direction direction = null;
        for (Direction value : MathUtils.HORIZONTAL) {
            BlockPos offset = bedrockPos.offset(value);
            BlockState blockState = world.getBlockState(offset);
            if (isReplaceableBlock(blockState)) {
                direction = value;
                continue;
            }
            BlockExcavator blockExcavator = FetcherUtils.getBlockExcavator(fakePlayer);
            if (blockState.isOf(Blocks.LEVER)) {
                // 拉杆没有附着在墙壁上，破坏拉杆
                if (blockState.get(WallMountedBlock.FACE) != BlockFace.WALL) {
                    return tickBreakBlock(blockExcavator, offset);
                }
                if (bedrockPos.equals(offset.offset(blockState.get(LeverBlock.FACING), -1))) {
                    if (context.getLeverPos() == null) {
                        context.setLeverPos(offset);
                        if (blockState.get(LeverBlock.POWERED)) {
                            continue;
                        }
                        // 激活拉杆
                        interactionLever(offset);
                    } else {
                        // 拉杆正确的附着在了基岩上，但是拉杆不止一个
                        return tickBreakBlock(blockExcavator, offset);
                    }
                } else {
                    BlockPos supportBlockPos = offset.offset(blockState.get(LeverBlock.FACING), -1);
                    // 拉杆附着在了另一个基岩上
                    if (world.getBlockState(supportBlockPos).isOf(Blocks.BEDROCK) && this.inSelectionArea(supportBlockPos)) {
                        continue;
                    }
                    // 拉杆附着在了墙上，但不是当前要破坏的基岩方块
                    return tickBreakBlock(blockExcavator, offset);
                }
            } else if (this.inSelectionArea(offset) && canMine(blockState, world, offset)) {
                return tickBreakBlock(blockExcavator, offset);
            }
        }
        if (context.getLeverPos() != null) {
            return StepResult.CONTINUE;
        }
        if (direction == null) {
            return StepResult.COMPLETION;
        }
        // 没有正确的拉杆附着在基岩上，放置并激活拉杆
        BlockPos offset = bedrockPos.offset(direction);
        BlockState blockState = world.getBlockState(offset.down());
        if (blockState.isOf(Blocks.PISTON) && blockState.get(PistonBlock.FACING) == Direction.UP) {
            // 当前位置下方是未伸出的活塞，不能在这里放置拉杆
            return StepResult.COMPLETION;
        }
        if (blockState.isOf(Blocks.MOVING_PISTON)) {
            // 当前位置下方是移动的活塞
            return StepResult.COMPLETION;
        }
        FakePlayerUtils.replenishment(fakePlayer, Hand.OFF_HAND, stack -> stack.isOf(Items.LEVER));
        FakePlayerUtils.look(fakePlayer, direction.getOpposite());
        BlockHitResult hitResult = new BlockHitResult(bedrockPos.toCenterPos(), direction, bedrockPos, false);
        // 放置拉杆
        interactionManager.interactBlock(fakePlayer, world, fakePlayer.getOffHandStack(), Hand.OFF_HAND, hitResult);
        // 再次单击激活拉杆
        interactionLever(offset);
        context.setLeverPos(offset);
        return StepResult.CONTINUE;
    }

    /**
     * 判断当前方块是否是可替换的，只对雪片进行了特判，其他带有{@code #minecraft:replaceable}标签的方块，例如藤蔓，发光地衣等会直接返回{@code true}
     *
     * @return 当前方块是否是可直接替换的
     */
    private boolean isReplaceableBlock(BlockState blockState) {
        if (blockState.isAir()) {
            return true;
        }
        if (blockState.isIn(BlockTags.REPLACEABLE)) {
            if (blockState.isOf(Blocks.SNOW)) {
                // 只有层数为1的雪片才能被替换
                return blockState.get(SnowBlock.LAYERS) == 1;
            }
            return true;
        }
        return false;
    }

    /**
     * 破除基岩
     */
    private StepResult pistonBreakBedrock(BedrockBreakingContext context) {
        BlockPos bedrockPos = context.getBedrockPos();
        BlockPos up = bedrockPos.up();
        // 基岩上方方块是活塞
        World world = this.getFakePlayer().getWorld();
        BlockState blockState = world.getBlockState(up);
        if (blockState.isOf(Blocks.PISTON) && blockState.get(PistonBlock.EXTENDED)) {
            BlockExcavator blockExcavator = FetcherUtils.getBlockExcavator(this.getFakePlayer());
            // 先切换工具，再计算剩余挖掘时间
            switchTool(blockState, world, up, this.getFakePlayer());
            // 计算剩余挖掘时间
            int currentTime = blockExcavator.computingRemainingMiningTime(up);
            if (currentTime == 1) {
                // 方块将在本游戏刻挖掘完毕
                BlockPos leverPos = context.getLeverPos();
                BlockState leverState = world.getBlockState(leverPos);
                if (leverState.isOf(Blocks.LEVER)) {
                    if (leverState.get(LeverBlock.POWERED)) {
                        // 关闭附着在基岩上的拉杆
                        interactionLever(leverPos);
                    }
                    // 关闭周围可能激活活塞的拉杆
                    closeTheSurroundingLevers(up);
                    context.setLeverPos(null);
                    // 继续挖掘，此时活塞应该会挖掘完毕
                    breakBlock(blockExcavator, up, false);
                    // 放置一个朝下的活塞，这个活塞会破坏掉基岩
                    if (placePiston(bedrockPos, Direction.DOWN).isAccepted()) {
                        return StepResult.COMPLETION;
                    }
                }
                return StepResult.COMPLETION;
            }
            breakBlock(blockExcavator, up, false);
            return StepResult.TICK_COMPLETION;
        } else {
            return StepResult.FAIL;
        }
    }

    /**
     * 关闭周围所有可能激活活塞的拉杆
     */
    private void closeTheSurroundingLevers(BlockPos pistonPos) {
        World world = this.getFakePlayer().getWorld();
        Consumer<BlockPos> consumer = blockPos -> {
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.isAir()) {
                return;
            }
            if (blockState.isOf(Blocks.LEVER) && blockState.get(LeverBlock.POWERED)) {
                interactionLever(blockPos);
            }
        };
        for (Direction direction : MathUtils.HORIZONTAL) {
            BlockPos offset = pistonPos.offset(direction);
            // 活塞周围的拉杆
            consumer.accept(offset);
            // 活塞周围的方块，这些方块可能是拉杆的支撑方块
            for (Direction value : MathUtils.HORIZONTAL) {
                consumer.accept(offset.offset(value));
            }
        }
        for (Direction direction : MathUtils.HORIZONTAL) {
            BlockPos offset = pistonPos.up().offset(direction);
            consumer.accept(offset);
            for (Direction value : MathUtils.HORIZONTAL) {
                consumer.accept(offset.offset(value));
            }
        }
        consumer.accept(pistonPos.up(2));
        for (Direction direction : MathUtils.HORIZONTAL) {
            consumer.accept(pistonPos.up(2).offset(direction));
        }
        // 活塞上方第三格可能放着朝下的拉杆
        BlockPos up = pistonPos.up(3);
        BlockState upBlockState = world.getBlockState(up);
        if (upBlockState.isOf(Blocks.LEVER) && upBlockState.get(LeverBlock.POWERED) && upBlockState.get(LeverBlock.FACE) == BlockFace.FLOOR) {
            interactionLever(up);
        }
        // 活塞下方第二格可能放着朝上的拉杆
        BlockPos down = pistonPos.down(2);
        BlockState downBlockState = world.getBlockState(down);
        if (downBlockState.isOf(Blocks.LEVER) && downBlockState.get(LeverBlock.POWERED) && downBlockState.get(LeverBlock.FACE) == BlockFace.CEILING) {
            interactionLever(down);
        }
    }

    /**
     * 挖掘掉破完基岩后留下的活塞
     */
    private boolean cleanPiston(BlockPos blockPos) {
        BlockExcavator blockExcavator = FetcherUtils.getBlockExcavator(this.getFakePlayer());
        BlockState blockState = this.getFakePlayer().getWorld().getBlockState(blockPos);
        if (blockState.isAir()) {
            return true;
        }
        // 移动的活塞
        if (blockState.isOf(Blocks.MOVING_PISTON)) {
            return false;
        }
        if (blockState.isOf(Blocks.PISTON)) {
            return breakBlock(blockExcavator, blockPos, true);
        }
        // 如果返回false，可能引发更多问题
        return true;
    }

    private StepResult tickBreakBlock(BlockExcavator blockExcavator, BlockPos blockPos) {
        EntityPlayerMPFake player = blockExcavator.getPlayer();
        World world = player.getWorld();
        if (FallingBlock.canFallThrough(world.getBlockState(blockPos.up()))) {
            // 等待上方下落的方块实体落下
            if (aboveHasFallingBlockEntity(world, blockPos)) {
                return StepResult.COMPLETION;
            }
        }
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.isOf(Blocks.TORCH) || blockState.isOf(Blocks.WALL_TORCH)) {
            // 挖掘火把上方的重力方块，只检查上方一格可能发生误判
            for (int i = 1; i <= 2; i++) {
                BlockPos up = blockPos.up(i);
                if (world.getBlockState(up).getBlock() instanceof FallingBlock) {
                    if (canInteract(up)) {
                        return breakBlock(blockExcavator, up, true) ? StepResult.COMPLETION : StepResult.TICK_COMPLETION;
                    } else {
                        return StepResult.COMPLETION;
                    }
                }
            }
        }
        if (world.getBlockState(blockPos).getBlock() instanceof FallingBlock) {
            // 如果有火把，挖掘最下方的重力方块并放置火把
            // 否则，挖掘最上方的重力方块
            boolean hasTorch = this.hasTorch();
            while (true) {
                Direction direction = hasTorch ? Direction.DOWN : Direction.UP;
                BlockPos offset = blockPos.offset(direction);
                if (world.getBlockState(offset).getBlock() instanceof FallingBlock && canInteract(offset.offset(direction))) {
                    blockPos = offset;
                } else {
                    break;
                }
            }
            boolean broken = breakBlock(blockExcavator, blockPos, true);
            if (broken && hasTorch) {
                FakePlayerUtils.replenishment(player, Hand.OFF_HAND, itemStack -> itemStack.isOf(Items.TORCH));
                placeBlock(blockPos);
                return StepResult.COMPLETION;
            } else {
                return StepResult.TICK_COMPLETION;
            }
        }
        return breakBlock(blockExcavator, blockPos, true) ? StepResult.COMPLETION : StepResult.TICK_COMPLETION;
    }

    /**
     * @return 玩家是否有火把
     */
    private boolean hasTorch() {
        return FakePlayerUtils.hasItem(this.getFakePlayer(), itemStack -> itemStack.isOf(Items.TORCH));
    }

    /**
     * @return 如果玩家有火把，返回上方是否有下落的方块，否则返回{@code false}
     */
    private boolean aboveHasFallingBlockEntity(World world, BlockPos blockPos) {
        return this.hasTorch() && BlockEntityIterator.ofAbove(world, blockPos, 0).contains(FallingBlockEntity.class);
    }

    /**
     * 尝试破坏指定位置的方块
     *
     * @param switchTool 是否需要切换工具
     * @return 是否破坏成功
     */
    private boolean breakBlock(BlockExcavator blockExcavator, BlockPos blockPos, boolean switchTool) {
        this.hasAction = true;
        EntityPlayerMPFake player = blockExcavator.getPlayer();
        World world = player.getWorld();
        BlockState blockState = world.getBlockState(blockPos);
        if (switchTool) {
            switchTool(blockState, world, blockPos, player);
        }
        return blockExcavator.mining(blockPos, Direction.DOWN, false);
    }

    private void switchTool(BlockState blockState, World world, BlockPos blockPos, EntityPlayerMPFake player) {
        boolean replenishment = FakePlayerUtils.replenishment(player, itemStack -> {
            if (player.isCreative()) {
                return itemStack.getItem().canMine(player.getMainHandStack(), blockState, world, blockPos, player);
            }
            if (itemStack.isEmpty()) {
                return false;
            }
            // 不使用低耐久工具
            if (isDamaged(itemStack)) {
                return false;
            }
            return itemStack.getMiningSpeedMultiplier(blockState) > 1F;
        });
        if (replenishment) {
            return;
        }
        // 工具没有切换成功，使用其他物品替换手上工具以避免工具损坏
        FakePlayerUtils.replenishment(this.getFakePlayer(), itemStack -> !isDamaged(itemStack));
    }

    /**
     * @return 指定物品是否是低耐久且有经验修补的物品
     */
    private boolean isDamaged(ItemStack itemStack) {
        return itemStack.isDamageable() && itemStack.getMaxDamage() - itemStack.getDamage() <= 10 && EnchantmentUtils.canRepairWithXp(itemStack);
    }

    /**
     * 放置活塞
     */
    private ActionResult placePiston(BlockPos bedrockPos, Direction direction) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        ServerPlayerInteractionManager interactionManager = fakePlayer.interactionManager;
        // 看向与活塞相反的方向
        FakePlayerUtils.look(fakePlayer, direction.getOpposite());
        FakePlayerUtils.replenishment(fakePlayer, Hand.OFF_HAND, itemStack -> itemStack.isOf(Items.PISTON));
        // 放置活塞
        BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(bedrockPos, 1.0), direction, bedrockPos.up(), false);
        ActionResult result = interactionManager.interactBlock(fakePlayer, fakePlayer.getWorld(), fakePlayer.getOffHandStack(), Hand.OFF_HAND, hitResult);
        if (result.isAccepted()) {
            this.hasAction = true;
        }
        return result;
    }

    /**
     * 放置一个方块
     */
    private void placeBlock(BlockPos blockPos) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        ServerPlayerInteractionManager interactionManager = fakePlayer.interactionManager;
        fakePlayer.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, blockPos.toCenterPos());
        BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(blockPos, 1.0), Direction.DOWN, blockPos, false);
        interactionManager.interactBlock(fakePlayer, fakePlayer.getWorld(), fakePlayer.getOffHandStack(), Hand.OFF_HAND, hitResult);
    }

    /**
     * 单击一次拉杆
     */
    private void interactionLever(BlockPos leverPos) {
        ServerPlayerInteractionManager interactionManager = this.getFakePlayer().interactionManager;
        BlockHitResult hitResult = new BlockHitResult(leverPos.toCenterPos(), Direction.UP, leverPos, false);
        ActionResult result = interactionManager.interactBlock(this.getFakePlayer(), this.getFakePlayer().getWorld(), this.getFakePlayer().getMainHandStack(), Hand.MAIN_HAND, hitResult);
        if (result.isAccepted()) {
            this.hasAction = true;
        }
    }

    /**
     * @return 是否需要进食
     */
    private boolean shouldEat() {
        if (FakePlayerUtils.hasItem(this.getFakePlayer(), InventoryUtils::isFoodItem)) {
            if (this.getFakePlayer().getAbilities().invulnerable) {
                return false;
            }
            HungerManager hungerManager = this.getFakePlayer().getHungerManager();
            if (hungerManager.getFoodLevel() <= 10) {
                return true;
            }
            if (hungerManager.isNotFull()) {
                return this.getFakePlayer().getMaxHealth() - this.getFakePlayer().getHealth() > 2;
            }
        }
        return false;
    }

    /**
     * 排除熔岩
     */
    private void drainFluidLava() {
        Iterator<BlockPos> iterator = this.lavas.iterator();
        while (iterator.hasNext()) {
            BlockPos blockPos = iterator.next();
            EntityPlayerMPFake fakePlayer = this.getFakePlayer();
            World world = fakePlayer.getWorld();
            if (this.canInteract(blockPos) &&
                (FakePlayerUtils.replenishment(fakePlayer, Hand.OFF_HAND, canDrainFluid(world, blockPos)) ||
                 FakePlayerUtils.replenishment(fakePlayer, Hand.OFF_HAND, itemStack -> itemStack.isOf(Items.PISTON)))) {
                placeBlock(blockPos);
            }
            iterator.remove();
        }
    }

    private Predicate<ItemStack> canDrainFluid(World world, BlockPos blockPos) {
        return itemStack -> {
            if (itemStack.isOf(Items.PISTON) || itemStack.isOf(Items.REDSTONE_BLOCK)) {
                return false;
            }
            if (itemStack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                // 避免放置潜影盒
                if (block instanceof BlockWithEntity) {
                    return false;
                }
                return block.getDefaultState().isSolidBlock(world, blockPos);
            }
            return false;
        };
    }

    /**
     * 吃食物
     */
    private void eat() {
        PlayerWorkPhase prev = this.prevPhase == PlayerWorkPhase.EAT ? PlayerWorkPhase.WORK : this.prevPhase;
        if (this.getFakePlayer().getAbilities().invulnerable) {
            this.setPhase(prev);
            return;
        }
        if (this.getFakePlayer().canConsume(false)) {
            if (this.getFakePlayer().getActiveItem().isEmpty()) {
                if (FakePlayerUtils.replenishment(this.getFakePlayer(), InventoryUtils::isFoodItem)) {
                    ServerPlayerInteractionManager interactionManager = this.getFakePlayer().interactionManager;
                    World world = this.getFakePlayer().getWorld();
                    ItemStack food = this.getFakePlayer().getMainHandStack();
                    interactionManager.interactItem(this.getFakePlayer(), world, food, Hand.MAIN_HAND);
                } else {
                    this.setPhase(prev);
                }
            }
        } else {
            this.setPhase(prev);
        }
    }

    /**
     * 收集材料
     */
    private void collectingMaterials() {
        boolean finished = this.pathfinder.isFinished();
        if (finished) {
            // 玩家可能在到达目标位置的前一瞬间捡起物品，导致在路径在走完之前被更新并不会执行到这里，但这不是问题
            dropGarbageAndCollectMaterial();
        }
        // 目标掉落物的位置不可到达
        if ((this.pathfinder.isInvalid() || this.pathfinder.isInaccessible()) && !this.itemEntities.isEmpty()) {
            this.itemEntities.remove(this.recentItemEntity);
            this.pathfinder.pause(1);
            this.materialCollectionComplete = true;
            this.recentItemEntity = null;
            // 要回收的最后一个物品不可到达，结束材料回收
            if (this.itemEntities.isEmpty()) {
                this.setPhase(PlayerWorkPhase.WORK);
                return;
            }
        }
        if (this.itemEntities.isEmpty()) {
            Box box = this.blockIterator.toBox().expand(10.0);
            List<ItemEntity> list = this.getFakePlayer().getWorld().getNonSpectatingEntities(ItemEntity.class, box)
                    .stream()
                    .filter(itemEntity -> isMaterial(itemEntity.getStack()))
                    .toList();
            this.itemEntities.addAll(list);
        }
        // 没有材料可以回收
        if (this.itemEntities.isEmpty()) {
            this.setPhase(PlayerWorkPhase.WORK);
            return;
        }
        if (this.recentItemEntity != null) {
            return;
        }
        ItemEntity recentEntity = this.itemEntities.getFirst();
        for (ItemEntity itemEntity : this.itemEntities) {
            if (recentEntity.distanceTo(getFakePlayer()) > itemEntity.distanceTo(getFakePlayer())) {
                recentEntity = itemEntity;
            }
        }
        this.recentItemEntity = recentEntity;
    }

    /**
     * 丢弃垃圾物品，然后把身上最多的材料放入潜影盒
     */
    private void dropGarbageAndCollectMaterial() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        PlayerInventory inventory = fakePlayer.getInventory();
        World world = fakePlayer.getWorld();
        BlockPos blockPos = fakePlayer.getBlockPos();
        ItemStack mostStack = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getMainStacks().size(); i++) {
            ItemStack itemStack = inventory.getMainStacks().get(i);
            if (itemStack.isEmpty()) {
                // 玩家物品栏里还有空槽位
                return;
            }
            if (itemStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().getDefaultState().isSolidBlock(world, blockPos)) {
                if (itemStack.getCount() > mostStack.getCount()) {
                    mostStack = itemStack;
                }
            }
        }
        final ItemStack finalMostStack = mostStack;
        boolean dropped = FakePlayerUtils.dropInventoryItem(fakePlayer, itemStack -> itemStack != finalMostStack && isGarbage(itemStack));
        if (dropped) {
            return;
        }
        if (CarpetOrgAdditionSettings.fakePlayerPickItemFromShulkerBox.get()) {
            this.collectMaterialToShulkerBox();
            this.collectToolToShulkerBox();
        }
    }

    /**
     * 把多余的材料装入潜影盒
     */
    private void collectMaterialToShulkerBox() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        PlayerInventory inventory = fakePlayer.getInventory();
        // 获取物品栏中数量最多的物品
        ItemStack most = InventoryUtils.findMostAbundantStack(inventory, this::isMaterial);
        PlayerScreenHandler screenHandler = fakePlayer.playerScreenHandler;
        // 丢弃一组最多的物品，预留一个空槽位，后面向堆叠的空潜影盒中放入物品时会用到
        // TODO 检查是否已经有空槽位了
        for (int i = FakePlayerUtils.PLAYER_INVENTORY_START; i <= FakePlayerUtils.PLAYER_INVENTORY_END; i++) {
            if (InventoryUtils.canMerge(most, screenHandler.getSlot(i).getStack())) {
                FakePlayerUtils.dropCursorStack(screenHandler, fakePlayer);
                FakePlayerUtils.throwItem(screenHandler, i, fakePlayer);
                break;
            }
        }
        // 整理物品栏
        FakePlayerUtils.sorting(fakePlayer);
        // 是否已经遍历到了数量最多的物品
        boolean isFoundMost = false;
        for (int i = FakePlayerUtils.PLAYER_INVENTORY_START; i <= FakePlayerUtils.PLAYER_INVENTORY_END; i++) {
            ItemStack itemStack = screenHandler.getSlot(i).getStack();
            // 因为已经整理过，所以遍历到了空物品或潜影盒，后面所有的物品都不是材料
            if (itemStack.isEmpty() || InventoryUtils.isShulkerBoxItem(itemStack)) {
                return;
            }
            if (InventoryUtils.canMerge(itemStack, most)) {
                isFoundMost = true;
                ItemStack result = InventoryUtils.putItemToInventoryShulkerBox(itemStack, fakePlayer);
                if (result.isEmpty()) {
                    continue;
                }
                FakePlayerUtils.putToEmptySlotOrDrop(fakePlayer, result);
                this.setPhase(PlayerWorkPhase.WORK);
                return;
            } else if (isFoundMost) {
                // 相同的材料是连续放置的，所以后面的物品都不是要装入潜影盒的材料
                return;
            }
        }
    }

    /**
     * 把损坏的工具放入潜影盒
     */
    private void collectToolToShulkerBox() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        // 整理物品栏
        FakePlayerUtils.sorting(fakePlayer);
        PlayerScreenHandler screenHandler = fakePlayer.playerScreenHandler;
        for (int i = FakePlayerUtils.PLAYER_INVENTORY_START; i <= FakePlayerUtils.PLAYER_INVENTORY_END; i++) {
            ItemStack itemStack = screenHandler.getSlot(i).getStack();
            if (itemStack.isEmpty() || InventoryUtils.isShulkerBoxItem(itemStack)) {
                return;
            }
            // 将已损坏的物品放入潜影盒
            if (InventoryUtils.isToolItem(itemStack) && isDamaged(itemStack)) {
                ItemStack result = InventoryUtils.putItemToInventoryShulkerBox(itemStack, fakePlayer);
                if (result.isEmpty()) {
                    continue;
                }
                FakePlayerUtils.putToEmptySlotOrDrop(fakePlayer, result);
                return;
            }
        }
    }

    private boolean isGarbage(ItemStack itemStack) {
        if (isMaterial(itemStack)) {
            return false;
        }
        if (itemStack.isOf(Items.TORCH)) {
            return false;
        }
        if (InventoryUtils.isFoodItem(itemStack)) {
            return false;
        }
        if (InventoryUtils.isShulkerBoxItem(itemStack)) {
            return false;
        }
        return !InventoryUtils.isToolItem(itemStack);
    }

    private boolean isMaterial(ItemStack itemStack) {
        return itemStack.isOf(Items.PISTON) || itemStack.isOf(Items.LEVER);
    }

    public void add(BedrockBreakingContext context) {
        this.contexts.add(context);
    }

    public void removeIf(Predicate<BedrockBreakingContext> predicate) {
        this.contexts.removeIf(predicate);
    }

    public boolean inSelectionArea(BlockPos blockPos) {
        return this.blockIterator.contains(blockPos);
    }

    public boolean isEmpty() {
        return this.contexts.isEmpty();
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.bedrock", getFakePlayer().getDisplayName()));
        switch (this.regionType) {
            case CUBOID -> {
                MutableText from = TextProvider.blockPos(this.blockIterator.getMinBlockPos(), Formatting.GREEN);
                MutableText to = TextProvider.blockPos(this.blockIterator.getMaxBlockPos(), Formatting.GREEN);
                list.add(TextBuilder.translate("carpet.commands.playerAction.info.bedrock.cuboid.range", from, to));
            }
            case CYLINDER -> {
                CylinderBlockIterator iterator = (CylinderBlockIterator) this.blockIterator;
                MutableText center = TextProvider.blockPos(iterator.center);
                list.add(TextBuilder.translate("carpet.commands.playerAction.info.bedrock.cylinder.center", center));
                list.add(TextBuilder.translate("carpet.commands.playerAction.info.bedrock.cylinder.radius", iterator.radius));
                list.add(TextBuilder.translate("carpet.commands.playerAction.info.bedrock.cylinder.height", iterator.height));
            }
        }
        if (this.ai) {
            list.add(TextBuilder.translate("carpet.commands.playerAction.info.bedrock.ai.enable"));
        }
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("region_type", this.regionType.name().toLowerCase());
        switch (this.regionType) {
            case CUBOID -> {
                BlockPos minBlockPos = this.blockIterator.getMinBlockPos();
                json.add("from", JsonUtils.toJson(minBlockPos));
                BlockPos maxBlockPos = this.blockIterator.getMaxBlockPos();
                json.add("to", JsonUtils.toJson(maxBlockPos));
            }
            case CYLINDER -> {
                CylinderBlockIterator iterator = (CylinderBlockIterator) this.blockIterator;
                json.add("center", JsonUtils.toJson(iterator.center));
                json.addProperty("radius", iterator.radius);
                json.addProperty("height", iterator.height);
            }
        }
        json.addProperty("ai", this.ai);
        json.addProperty("timed_material_recycling", this.recycleTimer != -1);
        return json;
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.bedrock");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.BEDROCK;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    protected void onAssignPlayer() {
        this.pathfinder = FakePlayerPathfinder.of(this::getFakePlayer, this::getMovingTarget);
    }

    @Override
    protected void onClearPlayer() {
        this.pathfinder = FakePlayerPathfinder.EMPTY;
    }

    /**
     * 获取移动目标
     */
    public Optional<BlockPos> getMovingTarget() {
        switch (this.getPhase()) {
            case WORK -> {
                World world = this.getFakePlayer().getWorld();
                if (this.bedrockTarget == null) {
                    return Optional.empty();
                }
                BlockState blockState = world.getBlockState(this.bedrockTarget);
                if (blockState.isOf(Blocks.BEDROCK) || this.isMovingToNearbyBedrock) {
                    return Optional.of(this.bedrockTarget);
                }
                return Optional.empty();
            }
            case COLLECT -> {
                if (this.recentItemEntity == null || this.recentItemEntity.isRemoved()) {
                    return Optional.empty();
                }
                return Optional.of(this.recentItemEntity.getBlockPos());
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    @Override
    public void onStop() {
        this.pathfinder.onStop();
    }

    /**
     * 玩家当前任务的阶段
     */
    @NotNull
    private PlayerWorkPhase getPhase() {
        return phase;
    }

    private void setPhase(@NotNull PlayerWorkPhase phase) {
        this.phase = phase;
        if (this.recycleTimer != -1 && phase == PlayerWorkPhase.WORK) {
            this.recycleTimer = MATERIAL_RECYCLING_TIME;
        }
    }

    private static class CylinderBlockIterator extends BlockIterator {
        private final BlockPos center;
        private final int radius;
        private final int height;

        public CylinderBlockIterator(BlockPos center, int radius, int height) {
            super(
                    new BlockPos(center.getX() - radius, center.getY(), center.getZ() - radius),
                    new BlockPos(center.getX() + radius, center.getY() + height, center.getZ() + radius)
            );
            this.center = center;
            this.radius = radius;
            this.height = height;
        }

        @Override
        public boolean contains(BlockPos blockPos) {
            return super.contains(blockPos) && MathUtils.getCalculateBlockIntegerDistance(this.center, blockPos) <= this.radius;
        }

        @Override
        public BlockPos randomBlockPos() {
            while (true) {
                BlockPos blockPos = super.randomBlockPos();
                if (this.contains(blockPos)) {
                    return blockPos;
                }
            }
        }

        @Override
        @NotNull
        public Iterator<BlockPos> iterator() {
            return new Iterator<>() {
                private final Iterator<BlockPos> iterator = CylinderBlockIterator.super.iterator();
                private BlockPos blockPos;

                @Override
                public boolean hasNext() {
                    if (this.blockPos == null) {
                        while (this.iterator.hasNext()) {
                            BlockPos next = this.iterator.next();
                            if (contains(next)) {
                                this.blockPos = next;
                                break;
                            }
                        }
                        return false;
                    }
                    return true;
                }

                @Override
                public BlockPos next() {
                    BlockPos result = this.blockPos;
                    this.blockPos = null;
                    return result;
                }
            };
        }
    }
}
