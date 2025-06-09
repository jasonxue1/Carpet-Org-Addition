package org.carpetorgaddition.mixin.accessor;

import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContainerComponent.class)
public interface ContainerComponentAccessor {
    @Accessor("stacks")
    DefaultedList<ItemStack> getStacks();

    @Mutable
    @Accessor("stacks")
    void setStacks(DefaultedList<ItemStack> stacks);

    @Mutable
    @Accessor("hashCode")
    void setHashCode(int hashCode);
}
