package boat.carpetorgaddition.rule;

import net.minecraft.server.level.ServerPlayer;

public interface CustomRuleControl<T> {
    T getCustomRuleValue(ServerPlayer player);

    boolean allowCustomSwitch();
}
