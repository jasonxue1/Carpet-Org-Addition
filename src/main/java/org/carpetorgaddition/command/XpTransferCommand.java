package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.OperationTimeoutException;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.ExperienceTransfer;

import java.math.BigInteger;

public class XpTransferCommand extends AbstractServerCommand {
    public XpTransferCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandXpTransfer))
                .then(CommandManager.argument("from", EntityArgumentType.player())
                        .then(CommandManager.argument("to", EntityArgumentType.player())
                                .then(CommandManager.literal("all")
                                        .executes(this::transferAll))
                                .then(CommandManager.literal("half")
                                        .executes(this::transferHalf))
                                .then(CommandManager.literal("points")
                                        .then(CommandManager.argument("number", IntegerArgumentType.integer(1))
                                                .executes(this::transferSpecifyPoint)))
                                .then(CommandManager.literal("level")
                                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                                .executes(this::transferSpecifyLevel)))
                                .then(CommandManager.literal("upgrade")
                                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                                .executes(this::transferUpgrade)))
                                .then(CommandManager.literal("upgradeto")
                                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                                .executes(this::transferUpgradeTo))))));
    }

    /**
     * 转移所有经验
     */
    private int transferAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ExperienceTransfer from = new ExperienceTransfer(getFromPlayer(context));
        ExperienceTransfer to = new ExperienceTransfer(getToPlayer(context));
        // 输出经验的玩家必须是假玩家或者是命令执行者自己
        if (from.isSpecifiedOrFakePlayer(CommandUtils.getSourcePlayer(context))) {
            // 获取转移之前玩家的经验
            int fromBeforeLevel = from.getLevel();
            int toBeforeLevel = to.getLevel();
            BigInteger number;
            try {
                number = from.transferAllTo(to);
            } catch (OperationTimeoutException e) {
                throw CommandUtils.createOperationTimeoutException();
            }
            // 获取转移之后玩家的经验
            int fromCurrentLevel = from.getLevel();
            int toCurrentLevel = to.getLevel();
            MutableText message = TextUtils.translate(
                    "carpet.commands.xpTransfer.all",
                    from.player().getDisplayName(),
                    number.toString(),
                    to.player().getDisplayName()
            );
            MutableText hover = getHover(to.player(), toCurrentLevel, toBeforeLevel, from.player(), fromBeforeLevel, fromCurrentLevel);
            MessageUtils.sendMessage(source, TextUtils.hoverText(message, hover));
            writeLog(source, to.player(), from.player(), number);
            return number.intValue();
        } else {
            // 发送需要目标是自己或假玩家消息
            throw CommandUtils.createException("carpet.commands.xpTransfer.self_or_fake_player");
        }
    }

    /**
     * 转移一半经验
     */
    private int transferHalf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ExperienceTransfer from = new ExperienceTransfer(getFromPlayer(context));
        ExperienceTransfer to = new ExperienceTransfer(getToPlayer(context));
        if (from.isSpecifiedOrFakePlayer(CommandUtils.getSourcePlayer(context))) {
            // 获取转移之前玩家的经验
            int outputBeforeLevel = from.getLevel();
            int inputBeforeLevel = to.getLevel();
            BigInteger number;
            try {
                number = from.transferHalfTo(to);
            } catch (OperationTimeoutException e) {
                throw CommandUtils.createOperationTimeoutException();
            }
            // 获取转移之后玩家的经验
            int outputCurrentLevel = from.getLevel();
            int inputCurrentLevel = to.getLevel();
            MutableText hover = getHover(to.player(), inputCurrentLevel, inputBeforeLevel, from.player(), outputBeforeLevel, outputCurrentLevel);
            MutableText message = TextUtils.translate(
                    "carpet.commands.xpTransfer.half",
                    from.player().getDisplayName(),
                    number.toString(),
                    to.player().getDisplayName()
            );
            MessageUtils.sendMessage(source, TextUtils.hoverText(message, hover));
            writeLog(source, to.player(), from.player(), number);
            return number.intValue();
        } else {
            // 发送消息：只允许操作自己或假玩家
            throw CommandUtils.createException("carpet.commands.xpTransfer.self_or_fake_player");
        }
    }

    /**
     * 转移指定数量的经验
     */
    private int transferSpecifyPoint(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int count = IntegerArgumentType.getInteger(context, "number");
        return this.transfer(context, BigInteger.valueOf(count));
    }

    /**
     * 转移从0级升级到指定等级所需的经验
     */
    private int transferSpecifyLevel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int level = IntegerArgumentType.getInteger(context, "level");
        BigInteger count;
        try {
            count = ExperienceTransfer.calculateTotalExperience(level, 0);
        } catch (ArithmeticException e) {
            throw CommandUtils.createException(e, "carpet.commands.xpTransfer.calculate.fail");
        }
        return this.transfer(context, count);
    }

    /**
     * 转移从当前等级<b>升级</b>指定等级所需的经验
     */
    private int transferUpgrade(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int level = getToPlayer(context).experienceLevel;
        int upgrade = level + IntegerArgumentType.getInteger(context, "level");
        BigInteger count;
        try {
            count = ExperienceTransfer.calculateUpgradeExperience(level, upgrade);
        } catch (ArithmeticException e) {
            throw CommandUtils.createException(e, "carpet.commands.xpTransfer.calculate.fail");
        }
        return this.transfer(context, count);
    }

    /**
     * 转移从当前等级<b>升级到</b>指定等级所需的经验
     */
    private int transferUpgradeTo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int level = getToPlayer(context).experienceLevel;
        int upgrade = IntegerArgumentType.getInteger(context, "level");
        BigInteger count;
        try {
            count = ExperienceTransfer.calculateUpgradeExperience(level, upgrade);
        } catch (ArithmeticException e) {
            throw CommandUtils.createException(e, "carpet.commands.xpTransfer.calculate.fail");
        }
        if (count.compareTo(BigInteger.ZERO) < 0) {
            // 升级所需经验为负数
            throw CommandUtils.createException("carpet.commands.xpTransfer.upgradeto.negative");
        }
        return this.transfer(context, count);
    }

    /**
     * 转移指定数量的经验
     */
    private int transfer(CommandContext<ServerCommandSource> context, BigInteger count) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ExperienceTransfer from = new ExperienceTransfer(getFromPlayer(context));
        ExperienceTransfer to = new ExperienceTransfer(getToPlayer(context));
        // 只能操作自己或假玩家
        if (from.isSpecifiedOrFakePlayer(CommandUtils.getSourcePlayer(context))) {
            // 获取转移之前玩家的经验
            int outputBeforeLevel = from.getLevel();
            int inputBeforeLevel = to.getLevel();
            try {
                from.transferTo(to, count);
            } catch (ExperienceTransfer.ExperienceTransferException e) {
                // 要转移经验的数量不能多于玩家的总经验
                throw CommandUtils.createException(
                        "carpet.commands.xpTransfer.point.fail",
                        from.player().getDisplayName(),
                        e.getRequire().toString(),
                        e.getExisting().toString()
                );
            } catch (OperationTimeoutException e) {
                throw CommandUtils.createOperationTimeoutException();
            }
            // 获取转移之后玩家的经验
            int outputCurrentLevel = from.getLevel();
            int inputCurrentLevel = to.getLevel();
            MutableText message = TextUtils.translate("carpet.commands.xpTransfer.point",
                    from.player().getDisplayName(),
                    count.toString(),
                    to.player().getDisplayName()
            );
            MutableText hover = getHover(to.player(), inputCurrentLevel, inputBeforeLevel, from.player(), outputBeforeLevel, outputCurrentLevel);
            MessageUtils.sendMessage(source, TextUtils.hoverText(message, hover));
            writeLog(source, to.player(), from.player(), count);
            return count.intValue();
        } else {
            // 发送消息：只允许操作自己或假玩家
            throw CommandUtils.createException("carpet.commands.xpTransfer.self_or_fake_player");
        }
    }

    // 获取要输出经验的玩家
    private ServerPlayerEntity getFromPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return EntityArgumentType.getPlayer(context, "from");
    }

    // 获取要输入经验的玩家
    private ServerPlayerEntity getToPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return EntityArgumentType.getPlayer(context, "to");
    }

    // 记录日志
    private void writeLog(
            ServerCommandSource source,
            ServerPlayerEntity inputPlayer,
            ServerPlayerEntity outputPlayer,
            BigInteger point
    ) {
        ServerPlayerEntity player = source.getPlayer();
        String output = player == outputPlayer ? "自己" : outputPlayer.getName().getString();
        String input = player == inputPlayer ? "自己" : inputPlayer.getName().getString();
        CarpetOrgAddition.LOGGER.info("{}将{}的{}点经验转移给{}", source.getName(), output, point.toString(), input);
    }

    // 获取悬停提示
    private MutableText getHover(
            ServerPlayerEntity inputPlayer,
            int inputCurrentLevel,
            int inputBeforeLevel,
            ServerPlayerEntity outputPlayer,
            int outputBeforeLevel,
            int outputCurrentLevel
    ) {
        return TextUtils.appendAll(
                TextUtils.setColor(
                        TextUtils.translate(
                                "carpet.commands.xpTransfer.upgrade",
                                inputPlayer.getDisplayName(),
                                inputCurrentLevel - inputBeforeLevel,
                                inputBeforeLevel,
                                inputCurrentLevel
                        ),
                        Formatting.GREEN
                ),
                TextProvider.NEW_LINE,
                TextUtils.setColor(
                        TextUtils.translate(
                                "carpet.commands.xpTransfer.degrade",
                                outputPlayer.getDisplayName(),
                                outputBeforeLevel - outputCurrentLevel,
                                outputBeforeLevel,
                                outputCurrentLevel
                        ),
                        Formatting.RED
                )
        );
    }

    @Override
    public String getDefaultName() {
        return "xpTransfer";
    }
}
