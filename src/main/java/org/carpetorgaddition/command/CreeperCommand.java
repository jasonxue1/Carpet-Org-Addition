package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.task.CreeperExplosionTask;
import org.carpetorgaddition.periodic.task.ServerTaskManager;
import org.carpetorgaddition.util.CommandUtils;

public class CreeperCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal(CommandConstants.CREEPER_COMMAND)
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandCreeper))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(CreeperCommand::creeperExplosion)));
    }

    // 创建苦力怕并爆炸
    private static int creeperExplosion(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity targetPlayer = CommandUtils.getArgumentPlayer(context);
        ServerTaskManager manager = ServerComponentCoordinator.getManager(context).getServerTaskManager();
        // 添加苦力怕爆炸任务
        manager.addTask(new CreeperExplosionTask(targetPlayer));
        ServerPlayerEntity sourcePlayer = context.getSource().getPlayer();
        if (sourcePlayer != null) {
            CarpetOrgAddition.LOGGER.info(
                    "{}在{}周围制造了一场苦力怕爆炸",
                    sourcePlayer.getName().getString(),
                    targetPlayer.getName().getString()
            );
        }
        return 1;
    }
}
