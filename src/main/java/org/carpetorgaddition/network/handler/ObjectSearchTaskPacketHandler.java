package org.carpetorgaddition.network.handler;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.network.c2s.ObjectSearchTaskC2SPacket;
import org.carpetorgaddition.network.codec.ObjectSearchTaskCodecs;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.periodic.task.ServerTaskManager;
import org.carpetorgaddition.periodic.task.search.BlockSearchTask;
import org.carpetorgaddition.periodic.task.search.ItemSearchTask;
import org.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import org.carpetorgaddition.periodic.task.search.TradeItemSearchTask;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.BlockEntityRegion;
import org.carpetorgaddition.wheel.BlockRegion;
import org.carpetorgaddition.wheel.ItemStackPredicate;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.List;

public class ObjectSearchTaskPacketHandler implements ServerPlayNetworking.PlayPayloadHandler<ObjectSearchTaskC2SPacket> {
    @Override
    public void receive(ObjectSearchTaskC2SPacket packet, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = FetcherUtils.getServer(player);
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        ServerWorld world = FetcherUtils.getWorld(player);
        ServerCommandSource source = player.getCommandSource();
        BlockPos blockPos = player.getBlockPos();
        ServerTask serverTask = switch (packet.type()) {
            case ITEM -> {
                ObjectSearchTaskCodecs.ItemSearchContext decode = ObjectSearchTaskCodecs.ITEM_SEARCH_CODEC.decode(packet.json());
                ItemStackPredicate predicate = new ItemStackPredicate(decode.list());
                BlockEntityRegion region = new BlockEntityRegion(world, blockPos, decode.range());
                yield new ItemSearchTask(world, predicate, region, source);
            }
            case OFFLINE_PLAYER_ITEM -> {
                ObjectSearchTaskCodecs.OfflinePlayerItemSearchContext decode = ObjectSearchTaskCodecs.OFFLINE_PLAYER_SEARCH__CODEC.decode(packet.json());
                ItemStackPredicate predicate = new ItemStackPredicate(decode.list());
                yield new OfflinePlayerSearchTask(source, predicate, player);
            }
            case BLOCK -> {
                ObjectSearchTaskCodecs.BlockSearchContext decode = ObjectSearchTaskCodecs.BLOCK_SEARCH_CODEC.decode(packet.json());
                BlockRegion region = new BlockRegion(world, blockPos, decode.range());
                FinderCommand.BlockPredicate predicate = createBlockPredicate(decode);
                yield new BlockSearchTask(world, blockPos, region, source, predicate);
            }
            case TRADE_ITEM -> {
                ObjectSearchTaskCodecs.TradeItemSearchContext decode = ObjectSearchTaskCodecs.TRADE_ITEM_SEARCH_CODEC.decode(packet.json());
                BlockRegion region = new BlockRegion(world, blockPos, decode.range());
                ItemStackPredicate predicate = new ItemStackPredicate(decode.list());
                yield new TradeItemSearchTask(world, region, blockPos, predicate, source);
            }
            default -> null;
        };
        if (serverTask == null) {
            return;
        }
        CommandUtils.handlingException(() -> taskManager.addTask(serverTask), source);
    }

    private FinderCommand.BlockPredicate createBlockPredicate(ObjectSearchTaskCodecs.BlockSearchContext decode) {
        return new FinderCommand.BlockPredicate() {
            private final List<Block> list = decode.list();

            @Override
            public boolean test(ServerWorld world, BlockPos pos) {
                Block block = world.getBlockState(pos).getBlock();
                return list.contains(block);
            }

            @Override
            public Text getName() {
                if (list.size() == 1) {
                    return list.getFirst().getName();
                }
                TextBuilder builder = new TextBuilder(list.getFirst().getName());
                builder.append("*").setItalic();
                return builder.build();
            }
        };
    }
}
