package org.carpetorgaddition.client.renderer.waypoint;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.client.util.ClientUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public abstract class WaypointIcon {
    /**
     * 路径点图标
     */
    private final Identifier icon;
    /**
     * 路径点剩余持续时间
     */
    private long remaining;
    /**
     * 路径点所在时间的注册表项
     */
    private final RegistryKey<World> worldKey;
    /**
     * 该路径点是否永久显示
     */
    private final boolean persistent;
    private float tickDelta = 0F;
    private float lastTickDelta = 0F;
    /**
     * 路径点消失时间
     */
    private static final long VANISHING_TIME = 4L;
    public static final Identifier HIGHLIGHT = Identifier.ofVanilla("textures/map/decorations/red_x.png");
    public static final Identifier NAVIGATOR = Identifier.ofVanilla("textures/map/decorations/target_x.png");

    protected WaypointIcon(World world, Identifier icon, long duration, boolean persistent) {
        this.worldKey = world.getRegistryKey();
        this.icon = icon;
        this.remaining = duration;
        this.persistent = persistent;
    }

    public static WaypointIcon ofHighlight(World world, long duration, boolean persistent) {
        return new Highlight(WaypointIcon.HIGHLIGHT, duration, world, persistent);
    }

    public static WaypointIcon ofNavigator(World world) {
        return new Navigator(WaypointIcon.NAVIGATOR, 1, world, true);
    }

    public void render(MatrixStack matrixStack, VertexConsumerProvider consumers, Vec3d target, Camera camera, RenderTickCounter tickCounter) {
        if (this.isDone()) {
            return;
        }
        if (!this.persistent || this.remaining <= 0) {
            float tickDelta = tickCounter.getTickDelta(false);
            if (this.lastTickDelta > tickDelta) {
                this.remaining--;
            }
            this.tickDelta = tickDelta;
            this.lastTickDelta = tickDelta;
        }
        // 获取摄像机位置
        Vec3d cameraPos = camera.getPos();
        // 玩家距离目标的位置
        Vec3d offset = target.subtract(cameraPos);
        // 获取客户端渲染距离
        int renderDistance = ClientUtils.getGameOptions().getViewDistance().getValue() * 16;
        // 修正路径点渲染位置
        Vec3d correction = new Vec3d(offset.getX(), offset.getY(), offset.getZ());
        if (correction.length() > renderDistance) {
            // 将路径点位置限制在渲染距离内
            correction = correction.normalize().multiply(renderDistance);
        }
        matrixStack.push();
        // 将路径点平移到方块位置
        matrixStack.translate(correction.getX(), correction.getY(), correction.getZ());
        float scale = this.getScale(correction.length());
        // 路径点大小
        matrixStack.scale(scale, scale, scale);
        // 翻转路径点
        matrixStack.multiply(new Quaternionf(-1, 0, 0, 0));
        // 让路径点始终对准玩家
        matrixStack.multiply(new Quaternionf().rotateY((float) ((Math.PI / 180.0) * (camera.getYaw() - 180F))));
        matrixStack.multiply(new Quaternionf().rotateX((float) ((Math.PI / 180.0) * (-camera.getPitch()))));
        MatrixStack.Entry entry = matrixStack.peek();
        Matrix4f matrix4f = entry.getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        // 绘制图标纹理
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix4f, -1F, -1F, 0F).texture(0, 0);
        bufferBuilder.vertex(matrix4f, -1F, 1F, 0F).texture(0, 1);
        bufferBuilder.vertex(matrix4f, 1F, 1F, 0F).texture(1, 1);
        bufferBuilder.vertex(matrix4f, 1F, -1F, 0F).texture(1, 0);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, this.icon);
        // 将缓冲区绘制到屏幕上。
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        tessellator.clear();
        // 如果准星正在指向路径点，显示文本
        if (isWatching(camera, target)) {
            drawDistance(matrixStack, consumers, offset, tessellator);
        }
        matrixStack.pop();
    }


    /**
     * 获取路径点大小
     *
     * @param distance 摄像机到路径点的距离，用来抵消远小近大
     */
    private float getScale(double distance) {
        if (this.isDone()) {
            return 0F;
        }
        // 修正路径点大小，使大小不会随着距离的改变而改变
        float scale = (float) distance / 30F;
        // 再次修正路径点大小，使随着距离的拉远路径点尺寸略微减小
        scale = Math.max(scale * (1F - (((float) distance / 40F) * 0.1F)), scale * 0.75F);
        if (this.remaining >= 0) {
            return scale;
        }
        // 播放消失动画
        return this.fade(VANISHING_TIME + (this.remaining - this.tickDelta) + 1, scale);
    }

    /**
     * 修正正在消失的路径点的大小
     *
     * @param time  剩余消失时间
     * @param scale 路径点的大小
     * @return 路径点的消失动画
     */
    private float fade(float time, float scale) {
        if (time <= 0L) {
            return 0F;
        }
        // 让消失动画先慢后快
        float x = time / VANISHING_TIME;
        // 消失动画（缩放）
        return scale * x * x;
    }

    /**
     * @return 光标是否指向路径点
     * @see EndermanEntity#isPlayerStaring(PlayerEntity)
     */
    @SuppressWarnings("JavadocReference")
    private boolean isWatching(Camera camera, Vec3d target) {
        float f = camera.getPitch() * (float) (Math.PI / 180.0);
        float g = -camera.getYaw() * (float) (Math.PI / 180.0);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        Vec3d vec3d = new Vec3d(i * j, -k, h * j).normalize();
        Vec3d vec3d2 = new Vec3d(target.getX() - camera.getPos().getX(), target.getY() - camera.getPos().getY(), target.getZ() - camera.getPos().getZ());
        double d = vec3d2.length();
        vec3d2 = vec3d2.normalize();
        double e = vec3d.dotProduct(vec3d2);
        return e > 0.999 - (0.025 / d);
    }

    /**
     * 绘制距离文本
     */
    private void drawDistance(MatrixStack matrixStack, VertexConsumerProvider consumers, Vec3d offset, Tessellator tessellator) {
        TextRenderer textRenderer = ClientUtils.getTextRenderer();
        // 计算距离
        double distance = offset.length();
        String formatted = distance >= 1000 ? "%.1fkm".formatted(distance / 1000) : "%.1fm".formatted(distance);
        TextBuilder builder = new TextBuilder(formatted);
        // 如果玩家与路径点不在同一纬度，设置距离文本为斜体
        if (!this.worldKey.equals(ClientUtils.getWorld().getRegistryKey())) {
            builder.setItalic();
        }
        // 获取文本宽度
        int width = textRenderer.getWidth(formatted);
        // 获取背景不透明度
        float backgroundOpacity = ClientUtils.getGameOptions().getTextBackgroundOpacity(0.25F);
        int opacity = (int) (backgroundOpacity * 255.0F) << 24;
        matrixStack.push();
        // 缩小文字
        matrixStack.scale(0.15F, 0.15F, 0.15F);
        // 渲染文字
        textRenderer.draw(builder.build(), -width / 2F, 8, Colors.WHITE, false,
                matrixStack.peek().getPositionMatrix(), consumers,
                TextRenderer.TextLayerType.SEE_THROUGH, opacity, 1);
        tessellator.clear();
        matrixStack.pop();
    }

    /**
     * 停止渲染并播放消失动画
     */
    public void stop() {
        if (this.remaining > 0L) {
            this.remaining = 0L;
        }
    }

    /**
     * @return 是否已经渲染完成，包括消失动画
     */
    public boolean isDone() {
        return -(this.remaining - 1) > VANISHING_TIME;
    }

    public Identifier getIcon() {
        return this.icon;
    }

    public abstract void clear();

    public abstract String getName();

    public static class Highlight extends WaypointIcon {
        protected Highlight(Identifier icon, long duration, World world, boolean persistent) {
            super(world, icon, duration, persistent);
        }

        @Override
        public void clear() {
/*            WorldRendererManager.remove(WaypointRenderer.class, );
            Consumer<Identifier> consumer = type -> WorldRendererManager.remove(WaypointRenderer.class, renderer -> renderer.getRenderType() == type);
            switch (this) {
                case HIGHLIGHT -> consumer.accept(HIGHLIGHT);
                // 请求服务器停止发送路径点更新数据包
                case NAVIGATOR -> {
                    ClientCommandUtils.sendCommand(CommandProvider.stopNavigate());
                    consumer.accept(NAVIGATOR);
                }
            }*/
            // TODO
        }

        @Override
        public String getName() {
            return "Highlight";
        }
    }

    public static class Navigator extends WaypointIcon {
        protected Navigator(Identifier icon, long duration, World world, boolean persistent) {
            super(world, icon, duration, persistent);
        }

        @Override
        public void clear() {
            // TODO
        }

        @Override
        public String getName() {
            return "Navigator";
        }
    }
}
