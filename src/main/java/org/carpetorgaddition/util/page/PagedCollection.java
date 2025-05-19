package org.carpetorgaddition.util.page;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.provider.CommandProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PagedCollection {
    private final ArrayList<Page> pages = new ArrayList<>();
    private final int id;

    public PagedCollection(int id) {
        this.id = id;
    }

    public void addContent(List<? extends Supplier<Text>> list) {
        int max = maximumNumberOfRow();
        int from = 0;
        while (from < list.size()) {
            int to = Math.min(from + max, list.size());
            this.pages.add(new Page(list, from, to));
            from += max;
        }
    }

    public void print(ServerCommandSource source) throws CommandSyntaxException {
        this.print(1, source);
    }

    public void print(int pagination, ServerCommandSource source) throws CommandSyntaxException {
        if (pagination <= 0 || pagination > this.pages.size()) {
            throw CommandUtils.createException("carpet.command.page.invalid", pagination, this.totalPages());
        }
        if (this.totalPages() == 1) {
            // 只有一页
            getPage(pagination).print(source);
        } else {
            getPage(pagination).print(source);
            MutableText pageTurningButton = TextUtils.appendAll(
                    TextUtils.setColor(TextUtils.createText("  ======"), Formatting.DARK_GRAY),
                    this.prevPageButton(pagination),
                    " [",
                    TextUtils.setColor(TextUtils.createText(pagination), Formatting.GOLD),
                    "/",
                    TextUtils.setColor(TextUtils.createText(this.totalPages()), Formatting.GOLD),
                    "] ",
                    this.nextPageButton(pagination),
                    TextUtils.setColor(TextUtils.createText("======"), Formatting.DARK_GRAY)
            );
            MessageUtils.sendMessage(source, pageTurningButton);
        }
    }

    private Text prevPageButton(int pagination) {
        MutableText prev = TextUtils.createText(" <<< ");
        if (pagination == 1) {
            // 已经是第一页，没有上一页了
            return TextUtils.setColor(prev, Formatting.GRAY);
        } else {
            MutableText hover = TextUtils.translate("carpet.command.page.prev");
            return TextUtils.command(prev, CommandProvider.pageTurning(this.id, pagination - 1), hover, Formatting.AQUA);
        }
    }

    private Text nextPageButton(int pagination) {
        MutableText next = TextUtils.createText(" >>> ");
        if (pagination == this.totalPages()) {
            // 已经是最后一页，没有下一页了
            return TextUtils.setColor(next, Formatting.GRAY);
        } else {
            MutableText hover = TextUtils.translate("carpet.command.page.next");
            return TextUtils.command(next, CommandProvider.pageTurning(this.id, pagination + 1), hover, Formatting.AQUA);
        }
    }

    private Page getPage(int pagination) {
        return this.pages.get(pagination - 1);
    }

    private int totalPages() {
        return this.pages.size();
    }


    public static int maximumNumberOfRow() {
        return Math.max(CarpetOrgAdditionSettings.finderCommandMaxFeedbackCount, 1);
    }
}
