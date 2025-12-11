package org.carpetorgaddition.rule;

import net.minecraft.server.level.ServerPlayer;

public abstract class CustomRuleControl<T> {
    public abstract T getRuleValue(ServerPlayer player);

    public abstract boolean isServerDecision();
}
