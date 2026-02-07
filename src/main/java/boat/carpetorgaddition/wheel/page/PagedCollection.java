package boat.carpetorgaddition.wheel.page;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.network.event.CustomClickAction;
import boat.carpetorgaddition.network.event.CustomClickEvents;
import boat.carpetorgaddition.network.event.CustomClickKeys;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class PagedCollection implements Iterable<Page> {
    private final ArrayList<Page> pages = new ArrayList<>();
    private final MinecraftServer server;
    private final int id;
    private final CommandSourceStack source;
    private int length = 0;

    public PagedCollection(MinecraftServer server, int id, CommandSourceStack source) {
        this.server = server;
        this.id = id;
        this.source = source;
    }

    public void addContent(List<? extends Supplier<Component>> list) {
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
            throw CommandUtils.createException(LocalizationKeys.Operation.Page.INVALID_INDEX.translate(pagination, this.totalPages()));
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
            list.add(new TextBuilder("  ======").setColor(ChatFormatting.DARK_GRAY));
            list.add(this.prevPageButton(pagination));
            list.add(" [");
            list.add(new TextBuilder(pagination).setColor(ChatFormatting.GOLD));
            list.add("/");
            list.add(new TextBuilder(this.totalPages()).setColor(ChatFormatting.GOLD));
            list.add("] ");
            list.add(this.nextPageButton(pagination));
            list.add(new TextBuilder("======").setColor(ChatFormatting.DARK_GRAY));
            Component pageTurningButton = TextBuilder.combineList(list);
            MessageUtils.sendMessage(this.source, pageTurningButton);
        }
    }

    private Component prevPageButton(int pagination) {
        TextBuilder builder = new TextBuilder(" <<< ");
        if (pagination == 1) {
            // 已经是第一页，没有上一页了
            builder.setColor(ChatFormatting.GRAY);
        } else {
            builder.setHover(LocalizationKeys.Button.PREV_PAGE.translate());
            NbtWriter writer = new NbtWriter(this.server, CustomClickAction.CURRENT_VERSION);
            writer.putInt(CustomClickKeys.ID, this.id);
            writer.putInt(CustomClickKeys.PAGE_NUMBER, pagination - 1);
            builder.setCustomEvent(CustomClickEvents.TURN_THE_PAGE, writer);
            builder.setColor(ChatFormatting.AQUA);
        }
        return builder.build();
    }

    private Component nextPageButton(int pagination) {
        TextBuilder builder = new TextBuilder(" >>> ");
        if (pagination == this.totalPages()) {
            // 已经是最后一页，没有下一页了
            builder.setColor(ChatFormatting.GRAY);
        } else {
            builder.setHover(LocalizationKeys.Button.NEXT_PAGE.translate());
            NbtWriter writer = new NbtWriter(this.server, CustomClickAction.CURRENT_VERSION);
            writer.putInt(CustomClickKeys.ID, this.id);
            writer.putInt(CustomClickKeys.PAGE_NUMBER, pagination + 1);
            builder.setCustomEvent(CustomClickEvents.TURN_THE_PAGE, writer);
            builder.setColor(ChatFormatting.AQUA);
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

    public CommandSourceStack getSource() {
        return this.source;
    }

    public static int maximumNumberOfRow() {
        return Math.max(CarpetOrgAdditionSettings.maxLinesPerPage.value(), 1);
    }

    @Override
    public @NotNull Iterator<Page> iterator() {
        return this.pages.iterator();
    }
}
