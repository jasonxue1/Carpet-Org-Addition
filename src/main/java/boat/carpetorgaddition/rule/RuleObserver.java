package boat.carpetorgaddition.rule;

import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface RuleObserver<T> {
    /**
     * @param source 规则的修改者
     * @param value  规则的新值
     * @return 是否可以修改，如果为{@code false}，则阻止规则修改
     */
    boolean onChanged(@Nullable CommandSourceStack source, T value);
}
