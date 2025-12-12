package boat.carpetorgaddition.mixin.accessor;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NaturalSpawner.SpawnState.class)
public interface SpawnHelperInfoAccessor {
    @Invoker("canSpawnForCategoryGlobal")
    boolean invokerIsBelowCap(MobCategory group);

    @Invoker("canSpawnForCategoryLocal")
    boolean invokerCanSpawn(MobCategory group, ChunkPos chunkPos);
}
