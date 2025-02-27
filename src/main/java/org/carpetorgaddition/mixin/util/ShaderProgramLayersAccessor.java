package org.carpetorgaddition.mixin.util;

import net.minecraft.client.gl.ShaderPipeline;
import net.minecraft.client.gl.ShaderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderPipelines.class)
public interface ShaderProgramLayersAccessor {
    @Accessor("RENDERTYPE_LINES")
    static ShaderPipeline.Stage getRenderTypeLines() {
        throw new AssertionError();
    }
}
