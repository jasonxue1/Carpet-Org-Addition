package org.carpetorgaddition.wheel.permission;

import net.minecraft.commands.CommandSourceStack;

import java.util.function.Predicate;

public class CommandPermission implements Predicate<CommandSourceStack> {
    private PermissionLevel level;

    /*package-private*/ CommandPermission(PermissionLevel level) {
        this.level = level;
    }

    @Override
    public boolean test(CommandSourceStack source) {
        return this.level.test(source);
    }

    public PermissionLevel getLevel() {
        return this.level;
    }

    public void setLevel(PermissionLevel level) {
        this.level = level;
    }
}
