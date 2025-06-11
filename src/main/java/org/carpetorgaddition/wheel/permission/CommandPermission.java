package org.carpetorgaddition.wheel.permission;

import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

public class CommandPermission implements Predicate<ServerCommandSource> {
    private PermissionLevel level;

    /*package-private*/ CommandPermission(PermissionLevel level) {
        this.level = level;
    }

    @Override
    public boolean test(ServerCommandSource source) {
        return this.level.test(source);
    }

    public PermissionLevel getLevel() {
        return this.level;
    }

    public void setLevel(PermissionLevel level) {
        this.level = level;
    }
}
