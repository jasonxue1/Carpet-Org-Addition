package org.carpetorgaddition.command;

import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableInt;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.mixin.rule.carpet.SettingsManagerAccessor;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.Collection;

public class RuleSearchCommand extends AbstractServerCommand {
    public RuleSearchCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        dispatcher.register(CommandManager.literal(name)
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandRuleSearch.get()))
                .then(CommandManager.argument("rule", StringArgumentType.greedyString())
                        .executes(this::listRule)));
    }

    // 列出符合条件的规则
    private int listRule(CommandContext<ServerCommandSource> context) {
        String filter = StringArgumentType.getString(context, "rule");
        if (filter.matches("\".*\"")) {
            filter = filter.substring(1, filter.length() - 1);
        }
        if (CarpetServer.settingsManager == null) {
            return 0;
        }
        MutableText text = TextBuilder.of("carpet.commands.ruleSearch.feedback", filter).setBold().build();
        // 将文本设置为粗体
        text.styled(style -> style.withBold(true));
        MessageUtils.sendMessage(context.getSource(), text);
        // 如果字符串为空，不搜索规则
        if (filter.isEmpty()) {
            return 0;
        }
        return listRule(context, filter);
    }

    private int listRule(CommandContext<ServerCommandSource> context, String filter) {
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
                    Text message = new TextBuilder(accessor.displayInteractiveSettings(rule)).setItalic().build();
                    MessageUtils.sendMessage(context.getSource(), message.copy());
                    ruleCount.increment();
                }
            }
        });
        return ruleCount.getValue();
    }

    @Override
    public String getDefaultName() {
        return "ruleSearch";
    }
}
