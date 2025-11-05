package org.carpetorgaddition.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ObjectShare;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.client.CarpetOrgAdditionClient;
import org.carpetorgaddition.client.command.argument.ClientBlockPosArgumentType;
import org.carpetorgaddition.client.renderer.waypoint.HighlightWaypoint;
import org.carpetorgaddition.client.renderer.waypoint.Waypoint;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRenderer;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class HighlightCommand extends AbstractClientCommand {
    public static final String DEFAULT_COMMAND_NAME = "highlight";

    public HighlightCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
        ObjectShare share = FabricLoader.getInstance().getObjectShare();
        Supplier<String> supplier = () -> {
            try {
                return getCustomNames()[0];
            } catch (RuntimeException e) {
                return DEFAULT_COMMAND_NAME;
            }
        };
        share.put("%s:%s".formatted(CarpetOrgAddition.MOD_ID, "highlight"), supplier);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(ClientCommandManager.literal(name)
                .then(ClientCommandManager.argument("blockPos", ClientBlockPosArgumentType.blockPos())
                        .executes(context -> highlight(context, 60 * 20L, !CarpetOrgAdditionClient.CLEAR_WAYPOINT.isUnbound()))
                        .then(ClientCommandManager.argument("second", IntegerArgumentType.integer(1))
                                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"30", "60", "120"}, builder))
                                .executes(context -> highlight(context, IntegerArgumentType.getInteger(context, "second") * 20L, false)))
                        .then(ClientCommandManager.literal("continue")
                                .executes(context -> highlight(context, 1L, true))))
                .then(ClientCommandManager.literal("clear")
                        .executes(context -> clear())));
    }

    // 高亮路径点
    private int highlight(CommandContext<FabricClientCommandSource> context, long duration, boolean persistent) {
        Vec3d vec3d = ClientBlockPosArgumentType.getBlockPos(context, "blockPos").toCenterPos();
        ClientWorld world = context.getSource().getWorld();
        WaypointRenderer instance = WaypointRenderer.getInstance();
        List<Waypoint> list = instance.listRenderers(Waypoint.HIGHLIGHT);
        Waypoint newWaypoint = new HighlightWaypoint(world, vec3d, duration, persistent);
        // 创建新路径点
        boolean update = false;
        for (Waypoint waypoint : list) {
            // 如果两个路径点指向同一个位置，就让玩家看向该路径点
            if (waypoint.equals(newWaypoint)) {
                update = true;
                // if语句结束后仍要设置新路径点，因为要重置持续时间
                context.getSource().getEntity().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, vec3d);
                break;
            }
        }
        // 设置新的路径点
        if (update) {
            instance.addOrUpdate(newWaypoint);
        } else {
            Optional<Waypoint> optional = instance.addOrModify(newWaypoint);
            optional.ifPresent(Waypoint::stop);
        }
        return 1;
    }

    // 取消高亮路径点
    private int clear() {
        WaypointRenderer instance = WaypointRenderer.getInstance();
        List<Waypoint> list = instance.listRenderers(Waypoint.HIGHLIGHT);
        int result = list.size();
        list.forEach(Waypoint::stop);
        return result;
    }

    @Override
    public String getDefaultName() {
        return DEFAULT_COMMAND_NAME;
    }
}
