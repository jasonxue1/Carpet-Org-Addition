package org.carpetorgaddition.mixin.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemContainerContents.class)
public interface ContainerComponentAccessor {
    @Invoker(value = "<init>", remap = false)
    static ItemContainerContents constructor(NonNullList<ItemStack> stacks) {
        throw new AssertionError();
    }
}
