package org.carpetorgaddition.exception;

import carpet.api.settings.InvalidRuleValueException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TranslatableInvalidRuleValueException extends InvalidRuleValueException {
    @Nullable
    private final Text message;

    public TranslatableInvalidRuleValueException() {
        this.message = null;
    }

    public TranslatableInvalidRuleValueException(@NotNull Text message) {
        this.message = message;
    }

    @Override
    public void notifySource(String ruleName, ServerCommandSource source) {
        if (this.message == null) {
            return;
        }
        MessageUtils.sendErrorMessage(source, this.message);
    }
}
