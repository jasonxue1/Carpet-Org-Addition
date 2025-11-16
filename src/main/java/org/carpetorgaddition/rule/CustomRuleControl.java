package org.carpetorgaddition.rule;

import net.minecraft.server.network.ServerPlayerEntity;

public abstract class CustomRuleControl<T> {
    public abstract T getRuleValue(ServerPlayerEntity player);

    public abstract boolean isServerDecision();
}
