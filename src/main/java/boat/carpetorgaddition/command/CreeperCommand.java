package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.task.CreeperExplosionTask;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.FetcherUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class CreeperCommand extends AbstractServerCommand {
    public CreeperCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandCreeper))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::creeperExplosion)));
    }

    // 创建苦力怕并爆炸
    private int creeperExplosion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = CommandUtils.getArgumentPlayer(context);
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(context).getServerTaskManager();
        // 添加苦力怕爆炸任务
        CommandSourceStack source = context.getSource();
        manager.addTask(new CreeperExplosionTask(source, targetPlayer));
        ServerPlayer sourcePlayer = source.getPlayer();
        if (sourcePlayer != null) {
            CarpetOrgAddition.LOGGER.info(
                    "{}在{}周围制造了一场苦力怕爆炸",
                    FetcherUtils.getPlayerName(sourcePlayer),
                    FetcherUtils.getPlayerName(targetPlayer)
            );
        }
        return 1;
    }

    @Override
    public String getDefaultName() {
        return "creeper";
    }
}
