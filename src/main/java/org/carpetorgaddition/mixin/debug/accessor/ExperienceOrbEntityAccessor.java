package org.carpetorgaddition.mixin.debug.accessor;

import net.minecraft.world.entity.ExperienceOrb;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@OnlyDeveloped
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbEntityAccessor {
    @Accessor("count")
    int getPickingCount();
}
