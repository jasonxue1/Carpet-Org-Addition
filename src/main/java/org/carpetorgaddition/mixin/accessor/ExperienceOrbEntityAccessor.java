package org.carpetorgaddition.mixin.accessor;

import net.minecraft.entity.ExperienceOrbEntity;
import org.carpetorgaddition.debug.OnlyDeveloped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@OnlyDeveloped
@Mixin(ExperienceOrbEntity.class)
public interface ExperienceOrbEntityAccessor {
    @Accessor("pickingCount")
    int getPickingCount();
}
