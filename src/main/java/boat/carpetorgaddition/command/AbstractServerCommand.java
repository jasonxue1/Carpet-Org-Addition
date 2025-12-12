package boat.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

public abstract class AbstractServerCommand extends AbstractCommand {
    protected final CommandDispatcher<CommandSourceStack> dispatcher;
    protected final CommandBuildContext access;

    public AbstractServerCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        this.dispatcher = dispatcher;
        this.access = access;
    }

    @Override
    protected final String getEnvironment() {
        return "Server";
    }
}
