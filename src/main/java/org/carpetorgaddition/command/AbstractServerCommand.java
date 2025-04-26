package org.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;

public abstract class AbstractServerCommand extends AbstractCommand {
    protected final CommandDispatcher<ServerCommandSource> dispatcher;
    protected final CommandRegistryAccess access;

    public AbstractServerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        this.dispatcher = dispatcher;
        this.access = access;
    }

    @Override
    protected final String getEnvironment() {
        return "Server";
    }
}
