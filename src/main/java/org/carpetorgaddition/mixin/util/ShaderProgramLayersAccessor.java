package org.carpetorgaddition.mixin.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.ShaderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderPipelines.class)
public interface ShaderProgramLayersAccessor {
    @Accessor("RENDERTYPE_LINES_SNIPPET")
    static RenderPipeline.Snippet getRenderTypeLines() {
        throw new AssertionError();
    }
}
