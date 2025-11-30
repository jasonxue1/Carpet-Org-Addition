package org.carpetorgaddition.network.handler;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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
import org.carpetorgaddition.wheel.traverser.BlockEntityTraverser;
import org.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import org.carpetorgaddition.wheel.permission.CommandPermission;
import org.carpetorgaddition.wheel.permission.PermissionManager;
import org.carpetorgaddition.wheel.predicate.BlockStatePredicate;
import org.carpetorgaddition.wheel.predicate.ItemStackPredicate;

public class ObjectSearchTaskPacketHandler implements ServerPlayNetworking.PlayPayloadHandler<ObjectSearchTaskC2SPacket> {
    @Override
    public void receive(ObjectSearchTaskC2SPacket packet, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        MinecraftServer server = FetcherUtils.getServer(player);
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        ServerWorld world = FetcherUtils.getWorld(player);
        ServerCommandSource source = player.getCommandSource();
        BlockPos blockPos = player.getBlockPos();
        try {
            checkPermission(packet.type(), source);
        } catch (CommandSyntaxException e) {
            CommandUtils.handlingException(e, source);
            return;
        }
        ServerTask serverTask = switch (packet.type()) {
            case ITEM -> {
                ObjectSearchTaskCodecs.ItemSearchContext decode = ObjectSearchTaskCodecs.ITEM_SEARCH_CODEC.decode(packet.json());
                ItemStackPredicate predicate = ItemStackPredicate.of(decode.list());
                BlockEntityTraverser traverser = new BlockEntityTraverser(world, blockPos, decode.range());
                yield new ItemSearchTask(world, predicate, traverser, source);
            }
            case OFFLINE_PLAYER_ITEM -> {
                ObjectSearchTaskCodecs.OfflinePlayerItemSearchContext decode = ObjectSearchTaskCodecs.OFFLINE_PLAYER_SEARCH_CODEC.decode(packet.json());
                ItemStackPredicate predicate = ItemStackPredicate.of(decode.list());
                yield new OfflinePlayerSearchTask(source, predicate, player);
            }
            case BLOCK -> {
                ObjectSearchTaskCodecs.BlockSearchContext decode = ObjectSearchTaskCodecs.BLOCK_SEARCH_CODEC.decode(packet.json());
                BlockPosTraverser traverser = new BlockPosTraverser(world, blockPos, decode.range());
                BlockStatePredicate predicate = BlockStatePredicate.ofBlocks(decode.list());
                yield new BlockSearchTask(world, blockPos, traverser, source, predicate);
            }
            case TRADE_ITEM -> {
                ObjectSearchTaskCodecs.TradeItemSearchContext decode = ObjectSearchTaskCodecs.TRADE_ITEM_SEARCH_CODEC.decode(packet.json());
                BlockPosTraverser traverser = new BlockPosTraverser(world, blockPos, decode.range());
                ItemStackPredicate predicate = ItemStackPredicate.of(decode.list());
                yield new TradeItemSearchTask(world, traverser, blockPos, predicate, source);
            }
            default -> null;
        };
        if (serverTask == null) {
            return;
        }
        CommandUtils.handlingException(() -> taskManager.addTask(serverTask), source);
    }

    private void checkPermission(ObjectSearchTaskC2SPacket.Type type, ServerCommandSource source) throws CommandSyntaxException {
        CommandPermission permission = switch (type) {
            case ITEM -> PermissionManager.getPermission(FinderCommand.FINDER_ITEM);
            case OFFLINE_PLAYER_ITEM -> PermissionManager.getPermission(FinderCommand.FINDER_ITEM_FROM_OFFLINE_PLAYER);
            case BLOCK -> PermissionManager.getPermission(FinderCommand.FINDER_BLOCK);
            default -> null;
        };
        if (permission == null || permission.test(source)) {
            return;
        }
        throw CommandUtils.createException("carpet.command.permission.insufficient");
    }
}
