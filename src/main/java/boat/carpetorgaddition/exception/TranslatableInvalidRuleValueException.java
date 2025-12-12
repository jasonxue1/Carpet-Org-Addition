package boat.carpetorgaddition.exception;

import boat.carpetorgaddition.util.MessageUtils;
import carpet.api.settings.InvalidRuleValueException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TranslatableInvalidRuleValueException extends InvalidRuleValueException {
    @Nullable
    private final Component message;

    public TranslatableInvalidRuleValueException() {
        this.message = null;
    }

    public TranslatableInvalidRuleValueException(@NotNull Component message) {
        this.message = message;
    }

    @Override
    public void notifySource(String ruleName, CommandSourceStack source) {
        if (this.message != null) {
            MessageUtils.sendErrorMessage(source, this.message);
        }
    }
}
