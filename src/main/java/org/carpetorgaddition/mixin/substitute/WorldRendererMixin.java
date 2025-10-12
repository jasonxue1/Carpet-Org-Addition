package org.carpetorgaddition.mixin.substitute;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.client.renderer.substitute.WorldRenderContext;
import org.carpetorgaddition.client.renderer.substitute.WorldRenderEvents;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;
    @Shadow
    @Final
    private DefaultFramebufferSet framebufferSet;
    @Unique
    private WorldRenderContext context;
    @Unique
    private MatrixStack matrixStack;

    @Inject(method = "render", at = @At("HEAD"))
    private void onStart(CallbackInfo ci) {
        WorldRenderEvents.START.invoker().onStart();
    }

    @WrapOperation(method = "method_62214", at = @At(value = "NEW", target = "()Lnet/minecraft/client/util/math/MatrixStack;"))
    private MatrixStack setMatrixStack(Operation<MatrixStack> original) {
        MatrixStack result = original.call();
        this.matrixStack = result;
        return result;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/FramePass;setRenderer(Ljava/lang/Runnable;)V"))
    private void setContext(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci, @Local Frustum frustum) {
        this.context = new WorldRenderContext(this.matrixStack, tickCounter, frustum, this.bufferBuilders.getEntityVertexConsumers());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getCloudRenderModeValue()Lnet/minecraft/client/option/CloudRenderMode;"))
    private void onAfterTranslucent(CallbackInfo ci, @Local FrameGraphBuilder frameGraphBuilder) {
        FramePass pass = frameGraphBuilder.createPass(CarpetOrgAddition.MOD_ID + ":afterTranslucent");
        this.framebufferSet.mainFramebuffer = pass.transfer(this.framebufferSet.mainFramebuffer);
        pass.setRenderer(() -> WorldRenderEvents.AFTER_TRANSLUCENT.invoker().render(this.context));
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/RenderDispatcher;render()V"))
    private void onDebug(CallbackInfo ci) {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.invoker().render(this.context);
    }
}
