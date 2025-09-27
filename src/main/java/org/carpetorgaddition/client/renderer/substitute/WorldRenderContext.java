package org.carpetorgaddition.client.renderer.substitute;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public record WorldRenderContext(
        MatrixStack matrixStack,
        RenderTickCounter tickCounter,
        Frustum frustum,
        VertexConsumerProvider consumers
) {
}
