package org.carpetorgaddition.client.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import org.carpetorgaddition.command.AbstractCommand;

public abstract class AbstractClientCommand extends AbstractCommand {
    protected final CommandDispatcher<FabricClientCommandSource> dispatcher;
    protected final CommandRegistryAccess access;

    public AbstractClientCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        this.dispatcher = dispatcher;
        this.access = access;
    }

    @Override
    protected final String getEnvironment() {
        return "Client";
    }
}
