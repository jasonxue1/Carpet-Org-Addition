package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.*;
import net.minecraft.command.argument.ItemPredicateArgumentType.ItemStackPredicateArgument;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.fakeplayer.action.*;
import org.carpetorgaddition.periodic.fakeplayer.action.bedrock.BedrockRegionType;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.ItemStackPredicate;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.permission.CommandPermission;
import org.carpetorgaddition.wheel.permission.PermissionLevel;
import org.carpetorgaddition.wheel.permission.PermissionManager;
import org.carpetorgaddition.wheel.screen.CraftingSetRecipeScreenHandler;
import org.carpetorgaddition.wheel.screen.StonecutterSetRecipeScreenHandler;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class PlayerActionCommand extends AbstractServerCommand {
    private final CommandPermission AI_PERMISSION = PermissionManager.registerHiddenCommand("playerAction.player.bedrock.ai", PermissionLevel.PASS);

    public PlayerActionCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandPlayerAction))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.literal("sorting")
                                .then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(this.access))
                                        .then(CommandManager.argument("this", Vec3ArgumentType.vec3())
                                                .then(CommandManager.argument("other", Vec3ArgumentType.vec3())
                                                        .executes(this::setCategorize)))))
                        .then(CommandManager.literal("empty")
                                .executes(context -> setEmptyTheContainer(context, true))
                                .then(CommandManager.argument("filter", ItemPredicateArgumentType.itemPredicate(this.access))
                                        .executes(context -> setEmptyTheContainer(context, false))))
                        .then(CommandManager.literal("fill")
                                .executes(context -> setFillTheContainer(context, true, true, false))
                                .then(CommandManager.argument("filter", ItemPredicateArgumentType.itemPredicate(this.access))
                                        .executes(context -> setFillTheContainer(context, false, true, false))
                                        .then(CommandManager.argument(FillTheContainerAction.DROP_OTHER, BoolArgumentType.bool())
                                                .executes(context -> setFillTheContainer(context, false, BoolArgumentType.getBool(context, FillTheContainerAction.DROP_OTHER), false))
                                                .then(CommandManager.argument(FillTheContainerAction.MORE_CONTAINER, BoolArgumentType.bool())
                                                        .executes(context -> setFillTheContainer(context, false, BoolArgumentType.getBool(context, FillTheContainerAction.DROP_OTHER), BoolArgumentType.getBool(context, FillTheContainerAction.MORE_CONTAINER)))))))
                        .then(CommandManager.literal("stop")
                                .executes(this::setStop))
                        .then(CommandManager.literal("craft")
                                .then(CommandManager.literal("one")
                                        .then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(this.access))
                                                .executes(this::setOneCraft)))
                                .then(CommandManager.literal("nine")
                                        .then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(this.access))
                                                .executes(this::setNineCraft)))
                                .then(CommandManager.literal("four")
                                        .then(CommandManager.argument("item", ItemPredicateArgumentType.itemPredicate(this.access))
                                                .executes(this::setFourCraft)))
                                .then(CommandManager.literal("crafting_table")
                                        .then(registerItemPredicateNode(9, this.access, this::setCraftingTableCraft)))
                                .then(CommandManager.literal("inventory")
                                        .then(registerItemPredicateNode(4, this.access, this::setInventoryCraft)))
                                .then(CommandManager.literal("gui")
                                        .executes(this::openFakePlayerCraftGui)))
                        .then(CommandManager.literal("trade")
                                .then(CommandManager.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> setTrade(context, false))
                                        .then(CommandManager.literal("void_trade")
                                                .executes(context -> setTrade(context, true)))))
                        .then(CommandManager.literal("info")
                                .executes(this::getAction))
                        .then(CommandManager.literal("rename")
                                .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(this.access))
                                        .then(CommandManager.argument("name", StringArgumentType.string())
                                                .executes(this::setRename))))
                        .then(CommandManager.literal("stonecutting")
                                .then(CommandManager.literal("item")
                                        .then(CommandManager.argument("item", ItemStackArgumentType.itemStack(this.access))
                                                .then(CommandManager.argument("button", IntegerArgumentType.integer(1))
                                                        .executes(this::setStonecutting))))
                                .then(CommandManager.literal("gui")
                                        .executes(this::useGuiSetStonecutting)))
                        .then(CommandManager.literal("fishing")
                                .executes(this::setFishing))
                        .then(CommandManager.literal("plant")
                                .requires(source -> CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION)
                                .executes(this::setPlant))
                        .then(register(CommandManager.literal("bedrock")
                                .requires(source -> CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION)))
                        .then(CommandManager.literal("goto")
                                .requires(source -> CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION)
                                .then(CommandManager.literal("block")
                                        .then(CommandManager.argument("target", BlockPosArgumentType.blockPos())
                                                .executes(this::setGotoBlockPos)))
                                .then(CommandManager.literal("entity")
                                        .then(CommandManager.argument("target", EntityArgumentType.entity())
                                                .executes(this::setGotoEntity))))));
    }

    private LiteralArgumentBuilder<ServerCommandSource> register(LiteralArgumentBuilder<ServerCommandSource> node) {
        for (BedrockRegionType value : BedrockRegionType.values()) {
            node.then(CommandManager.literal(value.name().toLowerCase(Locale.ROOT))
                    .then(register(value).apply(
                            CommandManager.argument("ai", BoolArgumentType.bool())
                                    .requires(AI_PERMISSION)
                                    .executes(context -> {
                                        boolean ai = BoolArgumentType.getBool(context, "ai");
                                        return setBreakBedrock(context, value, ai, false);
                                    })
                                    .then(CommandManager.argument("timedMaterialRecycling", BoolArgumentType.bool())
                                            .executes(context -> {
                                                boolean ai = BoolArgumentType.getBool(context, "ai");
                                                boolean timedMaterialRecycling = BoolArgumentType.getBool(context, "timedMaterialRecycling");
                                                return setBreakBedrock(context, value, ai, timedMaterialRecycling);
                                            })))));
        }
        return node;
    }

    private Function<RequiredArgumentBuilder<ServerCommandSource, ?>, RequiredArgumentBuilder<ServerCommandSource, ?>> register(BedrockRegionType value) {
        Command<ServerCommandSource> command = context -> setBreakBedrock(context, value, false, false);
        return switch (value) {
            case CUBOID -> argument ->
                    CommandManager.argument("from", BlockPosArgumentType.blockPos())
                            .then(CommandManager.argument("to", BlockPosArgumentType.blockPos())
                                    .executes(command)
                                    .then(argument));
            case CYLINDER -> argument ->
                    CommandManager.argument("center", BlockPosArgumentType.blockPos())
                            .then(CommandManager.argument("radius", IntegerArgumentType.integer(1))
                                    .then(CommandManager.argument("height", IntegerArgumentType.integer(1))
                                            .executes(command)
                                            .then(argument)));
        };
    }

    // 注册物品谓词节点
    private RequiredArgumentBuilder<ServerCommandSource, ItemStackPredicateArgument> registerItemPredicateNode(
            int maxValue,
            CommandRegistryAccess access,
            Command<ServerCommandSource> function
    ) {
        RequiredArgumentBuilder<ServerCommandSource, ItemStackPredicateArgument> result = null;
        for (int i = maxValue; i >= 1; i--) {
            var nobe = CommandManager.argument("item" + i, ItemPredicateArgumentType.itemPredicate(access));
            if (result == null) {
                result = nobe.executes(function);
            } else {
                nobe.then(result);
                result = nobe;
            }
        }
        return result;
    }

    // 设置停止
    private int setStop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        actionManager.stop();
        return 1;
    }

    // 设置物品分拣
    private int setCategorize(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        // 获取要分拣的物品对象
        ItemStackPredicate predicate = new ItemStackPredicate(context, "item");
        // 获取分拣物品要丢出的方向
        Vec3d thisVec = Vec3ArgumentType.getVec3(context, "this");
        // 获取非分拣物品要丢出的方向
        Vec3d otherVec = Vec3ArgumentType.getVec3(context, "other");
        actionManager.setAction(new ItemCategorizeAction(fakePlayer, predicate, thisVec, otherVec));
        return 1;
    }

    // 设置清空容器
    private int setEmptyTheContainer(CommandContext<ServerCommandSource> context, boolean allItem) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        ItemStackPredicate predicate = allItem ? ItemStackPredicate.WILDCARD : new ItemStackPredicate(context, "filter");
        actionManager.setAction(new EmptyTheContainerAction(fakePlayer, predicate));
        return 1;
    }

    // 设置填充容器
    private int setFillTheContainer(CommandContext<ServerCommandSource> context, boolean allItem, boolean dropOther, boolean moreContainer) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        ItemStackPredicate predicate = allItem ? ItemStackPredicate.WILDCARD : new ItemStackPredicate(context, "filter");
        actionManager.setAction(new FillTheContainerAction(fakePlayer, predicate, dropOther, moreContainer));
        return 1;
    }

    // 单个物品合成
    private int setOneCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "item");
        ItemStackPredicate[] predicates = fillArray(predicate, new ItemStackPredicate[4], false);
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        actionManager.setAction(new InventoryCraftAction(fakePlayer, predicates));
        return 1;
    }

    // 四个物品合成
    private int setFourCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "item");
        ItemStackPredicate[] predicates = fillArray(predicate, new ItemStackPredicate[4], true);
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        actionManager.setAction(new InventoryCraftAction(fakePlayer, predicates));
        return 1;
    }

    // 设置物品栏合成
    private int setInventoryCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        ItemStackPredicate[] items = new ItemStackPredicate[4];
        for (int i = 1; i <= 4; i++) {
            // 获取每一个合成材料
            items[i - 1] = new ItemStackPredicate(context, "item" + i);
        }
        actionManager.setAction(new InventoryCraftAction(fakePlayer, items));
        return 1;
    }

    // 九个物品合成
    private int setNineCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        ItemStackPredicate predicate = new ItemStackPredicate(context, "item");
        ItemStackPredicate[] predicates = fillArray(predicate, new ItemStackPredicate[9], true);
        actionManager.setAction(new CraftingTableCraftAction(fakePlayer, predicates));
        return 1;
    }

    // 设置工作台合成
    private int setCraftingTableCraft(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = prepareTheCrafting(context);
        ItemStackPredicate[] items = new ItemStackPredicate[9];
        for (int i = 1; i <= 9; i++) {
            items[i - 1] = new ItemStackPredicate(context, "item" + i);
        }
        actionManager.setAction(new CraftingTableCraftAction(fakePlayer, items));
        return 1;
    }

    // 设置交易
    private int setTrade(CommandContext<ServerCommandSource> context, boolean voidTrade) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        // 获取按钮的索引，减去1
        int index = IntegerArgumentType.getInteger(context, "index") - 1;
        actionManager.setAction(new TradeAction(fakePlayer, index, voidTrade));
        return 1;
    }

    // 设置重命名
    private int setRename(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        // 获取当前要操作的物品和要重命名的字符串
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        String newName = StringArgumentType.getString(context, "name");
        actionManager.setAction(new RenameAction(fakePlayer, item, newName));
        return 1;
    }

    // 设置使用切石机
    private int setStonecutting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        // 获取要切割的物品和按钮的索引
        Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        int buttonIndex = IntegerArgumentType.getInteger(context, "button") - 1;
        actionManager.setAction(new StonecuttingAction(fakePlayer, item, buttonIndex));
        return 1;
    }

    // 使用GUI设置使用切石机
    private int useGuiSetStonecutting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((i, inventory, playerEntity) -> {
            ScreenHandlerContext screenHandlerContext = ScreenHandlerContext.create(FetcherUtils.getWorld(player), player.getBlockPos());
            return new StonecutterSetRecipeScreenHandler(i, inventory, screenHandlerContext, fakePlayer);
        }, TextBuilder.translate("carpet.commands.playerAction.info.stonecutter.gui"));
        player.openHandledScreen(screen);
        return 1;
    }

    // 设置自动钓鱼
    private int setFishing(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        actionManager.setAction(new FishingAction(fakePlayer));
        return 1;
    }

    // 设置自动种植
    private int setPlant(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
            actionManager.setAction(new PlantAction(fakePlayer));
            return 1;
        }
        return 0;
    }

    // 设置破基岩
    private int setBreakBedrock(CommandContext<ServerCommandSource> context, BedrockRegionType regionType, boolean ai, boolean timedMaterialRecycling) throws CommandSyntaxException {
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            BedrockAction action = switch (regionType) {
                case CUBOID -> {
                    BlockPos from = BlockPosArgumentType.getBlockPos(context, "from");
                    BlockPos to = BlockPosArgumentType.getBlockPos(context, "to");
                    yield new BedrockAction(fakePlayer, from, to, ai, timedMaterialRecycling);
                }
                case CYLINDER -> {
                    BlockPos center = BlockPosArgumentType.getBlockPos(context, "center");
                    int radius = IntegerArgumentType.getInteger(context, "radius");
                    int height = IntegerArgumentType.getInteger(context, "height");
                    yield new BedrockAction(fakePlayer, center, radius, height, ai, timedMaterialRecycling);
                }
            };
            FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
            actionManager.setAction(action);
            Optional<ServerPlayerEntity> optional = CommandUtils.getSourcePlayerNullable(context);
            if (optional.isPresent()) {
                MutableText translate = TextBuilder.translate("carpet.commands.playerAction.bedrock.share");
                MessageUtils.sendMessageToHud(optional.get(), translate);
            }
            return 1;
        }
        return 0;
    }

    // 设置寻路到方块
    private int setGotoBlockPos(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            BlockPos target = BlockPosArgumentType.getBlockPos(context, "target");
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
            actionManager.setAction(new GotoAction(fakePlayer, target));
            return 1;
        }
        return 0;
    }

    // 设置寻路到实体
    private int setGotoEntity(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            Entity target = EntityArgumentType.getEntity(context, "target");
            EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
            FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
            actionManager.setAction(new GotoAction(fakePlayer, target));
            return 1;
        }
        return 0;
    }

    // 填充数组
    private ItemStackPredicate[] fillArray(ItemStackPredicate matcher, ItemStackPredicate[] matchers, boolean directFill) {
        if (directFill) {
            // 直接使用元素填满整个数组
            Arrays.fill(matchers, matcher);
        } else {
            // 第一个元素填入指定物品，其他元素填入空气
            for (int i = 0; i < matchers.length; i++) {
                if (i == 0) {
                    matchers[i] = matcher;
                } else {
                    matchers[i] = ItemStackPredicate.EMPTY;
                }
            }
        }
        return matchers;
    }

    // 获取假玩家操作类型
    private int getAction(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        AbstractPlayerAction action = actionManager.getAction();
        if (action.equalFakePlayer(null)) {
            action.setFakePlayer(fakePlayer);
        }
        MessageUtils.sendListMessage(context.getSource(), action.info());
        return 1;
    }

    // 打开控制假人合成物品的GUI
    private int openFakePlayerCraftGui(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        // 打开合成GUI
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity)
                -> new CraftingSetRecipeScreenHandler(i, playerInventory, fakePlayer,
                ScreenHandlerContext.create(FetcherUtils.getWorld(player), player.getBlockPos())),
                TextBuilder.translate("carpet.commands.playerAction.info.craft.gui"));
        player.openHandledScreen(screen);
        return 1;
    }

    // 在设置假玩家合成时获取动作管理器并提示启用合成修复
    private FakePlayerActionManager prepareTheCrafting(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        EntityPlayerMPFake fakePlayer = CommandUtils.getArgumentFakePlayer(context);
        return FetcherUtils.getFakePlayerActionManager(fakePlayer);
    }

    @Override
    public String getDefaultName() {
        return "playerAction";
    }
}
