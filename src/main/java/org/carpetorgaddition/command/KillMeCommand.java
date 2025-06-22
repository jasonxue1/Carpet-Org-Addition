package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;

// 自杀命令
public class KillMeCommand extends AbstractServerCommand {
    public KillMeCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandKillMe.get()))
                .executes(this::killMe));
    }

    // 玩家自杀
    private int killMe(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        //广播自杀消息
        try {
            CarpetOrgAdditionSettings.committingSuicide.set(true);
            player.kill(player.getWorld());
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
