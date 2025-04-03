package org.carpetorgaddition.command;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableInt;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.mixin.rule.carpet.SettingsManagerAccessor;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;

import java.util.Collection;

public class RuleSearchCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("ruleSearch")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandRuleSearch))
                .then(CommandManager.argument("rule", StringArgumentType.greedyString())
                        .executes(RuleSearchCommand::listRule)));
    }

    // 列出符合条件的规则
    private static int listRule(CommandContext<ServerCommandSource> context) {
        String filter = StringArgumentType.getString(context, "rule");
        if (filter.matches("\".*\"")) {
            filter = filter.substring(1, filter.length() - 1);
        }
        if (CarpetServer.settingsManager == null) {
            return 0;
        }
        MutableText text = TextUtils.toBold(TextUtils.translate("carpet.commands.ruleSearch.feedback", filter));
        // 将文本设置为粗体
        text.styled(style -> style.withBold(true));
        MessageUtils.sendMessage(context.getSource(), text);
        // 如果字符串为空，不搜索规则
        if (filter.isEmpty()) {
            return 0;
        }
        return listRule(context, filter);
    }

    private static int listRule(CommandContext<ServerCommandSource> context, String filter) {
        MutableInt ruleCount = new MutableInt(0);
        CarpetServer.forEachManager(settingsManager -> {
            SettingsManagerAccessor accessor = (SettingsManagerAccessor) settingsManager;
            Collection<CarpetRule<?>> rules = settingsManager.getCarpetRules();
            for (CarpetRule<?> rule : rules) {
                if (RuleHelper.translatedName(rule).contains(filter)) {
                    MessageUtils.sendMessage(context.getSource(), accessor.displayInteractiveSettings(rule));
                    ruleCount.increment();
                } else if (RuleHelper.translatedDescription(rule).contains(filter)) {
                    // 规则名中不包含字符串，但是规则描述中包含
                    Text message = accessor.displayInteractiveSettings(rule);
                    MessageUtils.sendMessage(context.getSource(), TextUtils.toItalic(message.copy()));
                    ruleCount.increment();
                }
            }
        });
        return ruleCount.getValue();
    }
}
