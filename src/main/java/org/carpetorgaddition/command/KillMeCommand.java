package org.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;

// 自杀命令
public class KillMeCommand extends AbstractServerCommand {
    public KillMeCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandKillMe))
                .executes(this::killMe));
    }

    // 玩家自杀
    private int killMe(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        //广播自杀消息
        try {
            CarpetOrgAdditionSettings.committingSuicide.set(true);
            player.kill(player.level());
        } finally {
            CarpetOrgAdditionSettings.committingSuicide.set(false);
        }
        return 1;
    }

    @Override
    public String getDefaultName() {
        return "killMe";
    }
}
