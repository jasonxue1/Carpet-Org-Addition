package org.carpetorgaddition.mixin.accessor;

import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.function.Function;

@Mixin(StateHolder.class)
public interface StateAccessor {
    @Accessor("PROPERTY_ENTRY_TO_STRING_FUNCTION")
    static Function<Map.Entry<Property<?>, Comparable<?>>, String> getPropertyMapPrinter() {
        throw new AssertionError();
    }
}
