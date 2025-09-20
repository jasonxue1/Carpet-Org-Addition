package org.carpetorgaddition.wheel.provider;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class TextProvider {
    /**
     * 换行
     */
    public static final Text NEW_LINE = TextBuilder.create("\n");
    public static final Text INDENT_SYMBOL = TextBuilder.create("    ");

    private TextProvider() {
    }

    /**
     * 主世界
     */
    public static final Text OVERWORLD = TextBuilder.translate("carpet.command.dimension.overworld");
    /**
     * 下界
     */
    public static final Text THE_NETHER = TextBuilder.translate("carpet.command.dimension.the_nether");
    /**
     * 末地
     */
    public static final Text THE_END = TextBuilder.translate("carpet.command.dimension.the_end");
    public static final Text TRUE = TextBuilder.translate("carpet.command.boolean.true");
    public static final Text FALSE = TextBuilder.translate("carpet.command.boolean.false");
    /**
     * [这里]
     */
    public static final Text CLICK_HERE = TextBuilder.translate("carpet.command.text.click.here");
    /**
     * 物品
     */
    public static final Text ITEM = TextBuilder.translate("carpet.command.item.item");
    /**
     * 单击复制到剪贴板
     *
     * @apiNote 玩家客户端中一定有这条消息，不需要回调
     */
    public static final Text COPY_CLICK = Text.translatable("chat.copy.click");
    /**
     * 自己
     */
    public static final Text SELF = TextBuilder.translate("carpet.command.text.self");

    public static Text getBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static Text blockPos(BlockPos blockPos) {
        return blockPos(blockPos, Formatting.GREEN);
    }

    /**
     * 获取一个方块坐标的可变文本对象，并带有点击复制、悬停文本，颜色效果
     *
     * @param color 文本的颜色，如果为null，不修改颜色
     */
    public static Text blockPos(BlockPos blockPos, @Nullable Formatting color) {
        TextBuilder builder = new TextBuilder(simpleBlockPos(blockPos));
        // 添加单击事件，复制方块坐标
        builder.setCopyToClipboard(WorldUtils.toPosString(blockPos));
        switch (CarpetOrgAdditionSettings.canHighlightBlockPos.get()) {
            case OMMC -> builder.append(new TextBuilder(" [H]")
                    .setCommand(CommandProvider.highlightWaypointByOmmc(blockPos))
                    .setHover("ommc.highlight_waypoint.tooltip"));
            case DEFAULT -> builder.append(new TextBuilder(" [H]")
                    .setCommand(CommandProvider.highlightWaypoint(blockPos))
                    .setHover("carpet.client.commands.highlight"));
            default -> {
            }
        }
        // 修改文本颜色
        builder.setColor(color);
        return builder.build();
    }

    /**
     * 返回一个简单的没有任何样式的方块坐标可变文本对象
     */
    public static Text simpleBlockPos(BlockPos blockPos) {
        return Texts.bracketed(Text.translatable("chat.coordinates", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
    }

    /**
     * 单击输入命令
     */
    @SuppressWarnings("unused")
    public static Text clickInput(String command) {
        return TextBuilder.translate("carpet.command.text.click.input", command);
    }

    /**
     * 单击执行命令
     *
     * @param command 要执行的命令
     */
    public static Text clickRun(String command) {
        TextBuilder builder = new TextBuilder(CLICK_HERE);
        builder.setCommand(command);
        builder.setHover("carpet.command.text.click.run", command);
        builder.setColor(Formatting.AQUA);
        return builder.build();
    }

    /**
     * 返回物品有几组几个
     *
     * @return {@code 物品组数}组{@code 物品个数}个
     */
    public static Text itemCount(int count, int maxCount) {
        // 计算物品有多少组
        int group = count / maxCount;
        // 计算物品余几个
        int remainder = count % maxCount;
        TextBuilder builder = new TextBuilder(count);
        // 为文本添加悬停提示
        if (group == 0) {
            builder.setHover("carpet.command.item.remainder", remainder);
        } else if (remainder == 0) {
            builder.setHover("carpet.command.item.group", group);
        } else {
            builder.setHover("carpet.command.item.count", group, remainder);
        }
        return builder.build();
    }

    /**
     * @param base 原始的文本对象
     * @return 获取物品栏中物品的名称和堆叠数量并用“*”连接，每个物品独占一行
     */
    public static Text inventory(Text base, Inventory inventory) {
        TextBuilder builder = new TextBuilder(base);
        ArrayList<Text> list = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            list.add(TextBuilder.combineAll(itemStack.getName(), "*", String.valueOf(itemStack.getCount())));
        }
        return builder.setHover(TextBuilder.joinList(list)).build();
    }

    /**
     * 将游戏刻时间转换为几分几秒的形式，如果时间非常接近整点，例如一小时零一秒，则会直接返回一小时，多出来的一秒会被忽略
     *
     * @param tick 游戏刻时间
     */
    public static Text tickToTime(long tick) {
        // 游戏刻
        if (tick < 20L) {
            return TextBuilder.translate("carpet.command.time.tick", tick);
        }
        // 秒
        if (tick < 1200L) {
            return TextBuilder.translate("carpet.command.time.second", tick / 20L);
        }
        // 整分
        if (tick < 72000L && (tick % 1200L == 0 || (tick / 20L) % 60L == 0)) {
            return TextBuilder.translate("carpet.command.time.minute", tick / 1200L);
        }
        // 分和秒
        if (tick < 72000L) {
            return TextBuilder.translate("carpet.command.time.minute_second", tick / 1200L, (tick / 20L) % 60L);
        }
        // 整小时
        if (tick % 72000L == 0 || (tick / 20L / 60L) % 60L == 0) {
            return TextBuilder.translate("carpet.command.time.hour", tick / 72000L);
        }
        // 小时和分钟
        return TextBuilder.translate("carpet.command.time.hour_minute", tick / 72000L, (tick / 20L / 60L) % 60L);
    }

    /**
     * 将当前系统时间偏移指定游戏刻数后返回时间的年月日时分秒形式
     *
     * @param offset 时间偏移的游戏刻数
     * @return 指定游戏刻之后的时间
     */
    public static Text tickToRealTime(long offset) {
        LocalDateTime time = LocalDateTime.now().plusSeconds(offset / 20);
        return TextBuilder.translate("carpet.command.time.format",
                time.getYear(), time.getMonth().ordinal() + 1, time.getDayOfMonth(),
                time.getHour(), time.getMinute(), time.getSecond());
    }

    /**
     * 获取维度名称
     *
     * @param world 要获取维度名称的世界对象
     * @return 如果是原版的3个维度，返回本Mod翻译后的名称，否则自己返回维度ID
     */
    public static Text dimension(World world) {
        String dimension = WorldUtils.getDimensionId(world);
        return switch (dimension) {
            case WorldUtils.OVERWORLD -> OVERWORLD;
            case WorldUtils.THE_NETHER -> THE_NETHER;
            case WorldUtils.THE_END -> THE_END;
            default -> TextBuilder.create(dimension);
        };
    }

    public static Text dimension(String dimension) {
        return switch (dimension) {
            case WorldUtils.OVERWORLD, WorldUtils.SIMPLE_OVERWORLD -> OVERWORLD;
            case WorldUtils.THE_NETHER, WorldUtils.SIMPLE_THE_NETHER -> THE_NETHER;
            case WorldUtils.THE_END, WorldUtils.SIMPLE_THE_END -> THE_END;
            default -> TextBuilder.create(dimension);
        };
    }
}
