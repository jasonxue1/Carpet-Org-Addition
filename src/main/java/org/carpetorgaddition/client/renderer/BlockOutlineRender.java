package org.carpetorgaddition.client.renderer;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.carpetorgaddition.client.util.ClientRenderUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

public class BlockOutlineRender {
    private final Tessellator tessellator = Tessellator.getInstance();
    private final BlockPos blockPos;
    private final VoxelShape voxelShape;

    public BlockOutlineRender(BlockPos blockPos) {
        this.blockPos = blockPos;
        ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        BlockState blockState = world.getBlockState(blockPos);
        this.voxelShape = blockState.getOutlineShape(world, this.blockPos);
    }

    public BlockOutlineRender(BlockPos blockPos, VoxelShape voxelShape) {
        this.blockPos = blockPos;
        this.voxelShape = voxelShape;
    }

    public void render(MatrixStack matrixStack) {
        if (voxelShape.isEmpty()) {
            return;
        }
        BufferBuilder bufferBuilder = this.tessellator.method_60827(VertexFormat.DrawMode.LINES, VertexFormats.LINE_COLOR_NORMAL);
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        matrixStack.push();
        matrixStack.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
        matrixStack.translate(this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ());
        MatrixStack.Entry entry = matrixStack.peek();
        Matrix4f matrix4f = entry.getPositionMatrix();
        // 渲染方块轮廓
        voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
            Vector3f vector3f = new Vector3f((float) (maxX - minX), (float) (maxY - minY), (float) (maxZ - minZ)).normalize();
            bufferBuilder.vertex(matrix4f, (float) minX, (float) minY, (float) minZ).color(0F, 0F, 1F, 1F).normal(entry, vector3f);
            bufferBuilder.vertex(matrix4f, (float) maxX, (float) maxY, (float) maxZ).color(0F, 0F, 1F, 1F).normal(entry, vector3f);
        });
        ClientRenderUtils.draw(RenderLayer.getLines(), bufferBuilder.end());
        matrixStack.pop();
    }
}
