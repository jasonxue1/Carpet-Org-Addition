package org.carpetorgaddition.wheel.page;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class PagedCollection implements Iterable<Page> {
    private final ArrayList<Page> pages = new ArrayList<>();
    private final int id;
    private final ServerCommandSource source;
    private int length = 0;

    public PagedCollection(int id, ServerCommandSource source) {
        this.id = id;
        this.source = source;
    }

    public void addContent(List<? extends Supplier<Text>> list) {
        int max = maximumNumberOfRow();
        int from = 0;
        while (from < list.size()) {
            int to = Math.min(from + max, list.size());
            this.pages.add(new Page(list, from, to));
            from += max;
        }
        this.length += list.size();
    }

    public void print() throws CommandSyntaxException {
        this.print(1, false);
    }

    public void print(int pagination, boolean printBlankLine) throws CommandSyntaxException {
        if (pagination <= 0 || pagination > this.pages.size()) {
            throw CommandUtils.createException("carpet.command.page.invalid", pagination, this.totalPages());
        }
        if (printBlankLine) {
            MessageUtils.sendEmptyMessage(this.source);
        }
        if (this.totalPages() == 1) {
            // 只有一页
            getPage(pagination).print(this.source);
        } else {
            getPage(pagination).print(this.source);
            ArrayList<Object> list = new ArrayList<>();
            list.add(new TextBuilder("  ======").setColor(Formatting.DARK_GRAY));
            list.add(this.prevPageButton(pagination));
            list.add(" [");
            list.add(new TextBuilder(pagination).setColor(Formatting.GOLD));
            list.add("/");
            list.add(new TextBuilder(this.totalPages()).setColor(Formatting.GOLD));
            list.add("] ");
            list.add(this.nextPageButton(pagination));
            list.add(new TextBuilder("======").setColor(Formatting.DARK_GRAY));
            MutableText pageTurningButton = TextBuilder.combineList(list);
            MessageUtils.sendMessage(this.source, pageTurningButton);
        }
    }

    private Text prevPageButton(int pagination) {
        TextBuilder builder = new TextBuilder(" <<< ");
        if (pagination == 1) {
            // 已经是第一页，没有上一页了
            builder.setColor(Formatting.GRAY);
        } else {
            builder.setHover("carpet.command.page.prev");
            builder.setCommand(CommandProvider.pageTurning(this.id, pagination - 1));
            builder.setColor(Formatting.AQUA);
        }
        return builder.build();
    }

    private Text nextPageButton(int pagination) {
        TextBuilder builder = new TextBuilder(" >>> ");
        if (pagination == this.totalPages()) {
            // 已经是最后一页，没有下一页了
            builder.setColor(Formatting.GRAY);
        } else {
            builder.setHover("carpet.command.page.next");
            builder.setCommand(CommandProvider.pageTurning(this.id, pagination + 1));
            builder.setColor(Formatting.AQUA);
        }
        return builder.build();
    }

    private Page getPage(int pagination) {
        return this.pages.get(pagination - 1);
    }

    /**
     * @return 页面的数量
     */
    public int totalPages() {
        return this.pages.size();
    }

    /**
     * @return 所有页面消息数量的总和
     */
    public int length() {
        return this.length;
    }

    public ServerCommandSource getSource() {
        return this.source;
    }

    public static int maximumNumberOfRow() {
        return Math.max(CarpetOrgAdditionSettings.finderCommandMaxFeedbackCount, 1);
    }

    @Override
    public @NotNull Iterator<Page> iterator() {
        return this.pages.iterator();
    }
}
