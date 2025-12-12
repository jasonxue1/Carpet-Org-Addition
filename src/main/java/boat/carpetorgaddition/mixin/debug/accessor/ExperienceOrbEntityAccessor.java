package boat.carpetorgaddition.mixin.debug.accessor;

import boat.carpetorgaddition.debug.OnlyDeveloped;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@OnlyDeveloped
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbEntityAccessor {
    @Accessor("count")
    int getPickingCount();
}
