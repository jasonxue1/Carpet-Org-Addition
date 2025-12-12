package boat.carpetorgaddition.wheel.page;

import boat.carpetorgaddition.util.MessageUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Supplier;

public class Page {
    private final List<? extends Supplier<Component>> list;
    private final int from;
    private final int to;

    public Page(List<? extends Supplier<Component>> list, int from, int to) {
        this.list = list;
        this.from = from;
        this.to = to;
    }

    public void print(CommandSourceStack source) {
        for (int i = this.from; i < this.to; i++) {
            MessageUtils.sendMessage(source, this.list.get(i).get());
        }
    }
}
