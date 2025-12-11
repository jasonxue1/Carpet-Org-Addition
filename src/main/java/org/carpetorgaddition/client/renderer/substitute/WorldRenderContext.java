package org.carpetorgaddition.client.renderer.substitute;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;

public record WorldRenderContext(
        PoseStack matrixStack,
        DeltaTracker tickCounter,
        Frustum frustum,
        MultiBufferSource consumers
) {
}
