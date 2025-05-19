package org.carpetorgaddition.util.page;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.MessageUtils;

import java.util.List;
import java.util.function.Supplier;

public class Page {
    private final List<? extends Supplier<Text>> list;
    private final int from;
    private final int to;

    public Page(List<? extends Supplier<Text>> list, int from, int to) {
        this.list = list;
        this.from = from;
        this.to = to;
    }

    public void print(ServerCommandSource source) {
        for (int i = this.from; i < this.to; i++) {
            MessageUtils.sendMessage(source, this.list.get(i).get());
        }
    }
}
