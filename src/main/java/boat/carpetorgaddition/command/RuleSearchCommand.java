package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.mixin.accessor.carpet.SettingsManagerAccessor;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.CarpetServer;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Collection;

public class RuleSearchCommand extends AbstractServerCommand {
    public RuleSearchCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandRuleSearch))
                .then(Commands.argument("rule", StringArgumentType.greedyString())
                        .executes(this::listRule)));
    }

    // 列出符合条件的规则
    private int listRule(CommandContext<CommandSourceStack> context) {
        String filter = StringArgumentType.getString(context, "rule");
        if (filter.matches("\".*\"")) {
            filter = filter.substring(1, filter.length() - 1);
        }
        Component text = TextBuilder.of("carpet.commands.ruleSearch.feedback", filter)
                .setBold()
                .build();
        MessageUtils.sendMessage(context.getSource(), text);
        // 如果字符串为空，不搜索规则
        if (filter.isEmpty()) {
            return 0;
        }
        return listRule(context, filter);
    }

    private int listRule(CommandContext<CommandSourceStack> context, String filter) {
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
                    Component message = new TextBuilder(accessor.displayInteractiveSettings(rule)).setItalic().build();
                    MessageUtils.sendMessage(context.getSource(), message);
                    ruleCount.increment();
                }
            }
        });
        return ruleCount.intValue();
    }

    @Override
    public String getDefaultName() {
        return "ruleSearch";
    }
}
