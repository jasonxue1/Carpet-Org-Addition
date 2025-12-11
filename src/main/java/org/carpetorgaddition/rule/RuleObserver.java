package org.carpetorgaddition.rule;

import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface RuleObserver<T> {
    void onChanged(@Nullable CommandSourceStack source, T value);
}
