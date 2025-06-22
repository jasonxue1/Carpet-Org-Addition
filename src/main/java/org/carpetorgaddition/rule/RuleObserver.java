package org.carpetorgaddition.rule;

import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface RuleObserver<T> {
    void onChanged(@Nullable ServerCommandSource source, T value);
}
