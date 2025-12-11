package org.carpetorgaddition.mixin.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPipelines.class)
public interface ShaderProgramLayersAccessor {
    @Accessor("LINES_SNIPPET")
    static RenderPipeline.Snippet getRenderTypeLines() {
        throw new AssertionError();
    }
}
