package org.carpetorgaddition.client.renderer.waypoint;

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
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Objects;

public abstract class Waypoint {
    /**
     * 路径点图标
     */
    private final Identifier icon;
    /**
     * 路径点已经显示的时间
     */
    private long age;
    /**
     * 路径点剩余持续时间
     */
    private long remaining;
    private Vec3d target;
    @NotNull
    protected Vec3d lastTarget;
    /**
     * 路径点所在时间的注册表项
     */
    @NotNull
    protected RegistryKey<World> registryKey;
    /**
     * 该路径点是否永久显示
     */
    private final boolean persistent;
    protected float tickDelta = 1F;
    protected float lastTickDelta = 1F;
    /**
     * 路径点消失时间
     */
    private static final long VANISHING_TIME = 4L;
    public static final Identifier HIGHLIGHT = Identifier.ofVanilla("textures/map/decorations/red_x.png");
    public static final Identifier NAVIGATOR = Identifier.ofVanilla("textures/map/decorations/target_x.png");

    public Waypoint(@NotNull RegistryKey<World> registryKey, @NotNull Vec3d target, Identifier icon, long duration, boolean persistent) {
        this.registryKey = registryKey;
        this.target = target;
        this.lastTarget = target;
        this.icon = icon;
        this.remaining = duration;
        this.persistent = persistent;
    }

    public Waypoint(World world, Vec3d target, Identifier icon, long duration, boolean persistent) {
        this(world.getRegistryKey(), target, icon, duration, persistent);
    }

    public void render(MatrixStack matrixStack, VertexConsumerProvider consumers, Camera camera, RenderTickCounter tickCounter) {
        if (this.isDone()) {
            return;
        }
        Vec3d revised = this.getRevisedPos();
        if (revised == null) {
            return;
        }
        float tickDelta = tickCounter.getTickProgress(false);
        if (this.tickDelta > tickDelta) {
            this.tick();
        }
        this.lastTickDelta = this.tickDelta;
        this.tickDelta = tickDelta;
        // 获取摄像机位置
        Vec3d cameraPos = camera.getPos();
        // 玩家距离目标的位置
        Vec3d offset = revised.subtract(cameraPos);
        // 获取客户端渲染距离
        int renderDistance = ClientUtils.getGameOptions().getViewDistance().getValue() * 16;
        // 修正路径点渲染位置
        Vec3d correction = new Vec3d(offset.getX(), offset.getY(), offset.getZ());
        if (correction.length() > renderDistance) {
            // 将路径点位置限制在渲染距离内
            correction = correction.normalize().multiply(renderDistance);
        }
        matrixStack.push();
        this.transform(matrixStack, camera, correction);
        this.render(matrixStack, consumers);
        // 如果准星正在指向路径点，显示文本
        if (isWatching(camera, revised)) {
            drawDistance(matrixStack, consumers, offset);
        }
        matrixStack.pop();
    }

    private void render(MatrixStack matrixStack, VertexConsumerProvider consumers) {
        RenderLayer renderLayer = RenderLayer.getFireScreenEffect(this.icon);
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        VertexConsumer vertexConsumer = consumers.getBuffer(renderLayer);
        vertexConsumer.vertex(matrix4f, -1F, -1F, 0F).texture(0F, 0F).color(-1);
        vertexConsumer.vertex(matrix4f, -1F, 1F, 0F).texture(0F, 1F).color(-1);
        vertexConsumer.vertex(matrix4f, 1F, 1F, 0F).texture(1F, 1F).color(-1);
        vertexConsumer.vertex(matrix4f, 1F, -1F, 0F).texture(1F, 0F).color(-1);
    }

    /**
     * 变换矩阵
     */
    protected void transform(MatrixStack matrixStack, Camera camera, Vec3d correction) {
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
    }

    protected void tick() {
        this.age++;
        if (!this.persistent || this.remaining <= 0) {
            this.remaining--;
        }
    }

    @Nullable
    protected Vec3d getRevisedPos() {
        // 获取玩家所在维度ID
        RegistryKey<World> key = ClientUtils.getWorld().getRegistryKey();
        // 玩家和路径点在同一维度
        Vec3d interpolation = getInterpolation();
        if (this.registryKey.equals(key)) {
            return interpolation;
        }
        Camera camera = ClientUtils.getCamera();
        // 玩家在主世界，路径点在下界，将路径点坐标换算成主世界坐标
        if (WorldUtils.isOverworld(key) && WorldUtils.isTheNether(this.registryKey)) {
            return new Vec3d(interpolation.getX() * 8, camera.getPos().getY(), interpolation.getZ() * 8);
        }
        // 玩家在下界，路径点在主世界，将路径点坐标换算成下界坐标
        if (WorldUtils.isTheNether(key) && WorldUtils.isOverworld(this.registryKey)) {
            return new Vec3d(interpolation.getX() / 8, camera.getPos().getY(), interpolation.getZ() / 8);
        }
        return null;
    }

    protected Vec3d getInterpolation() {
        return this.target;
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
        // 播放出场动画
        if (this.remaining < 0) {
            return this.fade(VANISHING_TIME + (this.remaining - this.tickDelta) + 1, scale);
        }
        // 播放入场动画
        if (this.age < VANISHING_TIME) {
            return this.fade(this.age + this.tickDelta, scale);
        }
        return scale;
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
    private void drawDistance(MatrixStack matrixStack, VertexConsumerProvider consumers, Vec3d offset) {
        TextRenderer textRenderer = ClientUtils.getTextRenderer();
        // 计算距离
        double distance = offset.length();
        String formatted = distance >= 1000 ? "%.1fkm".formatted(distance / 1000) : "%.1fm".formatted(distance);
        TextBuilder builder = new TextBuilder(formatted);
        // 如果玩家与路径点不在同一纬度，设置距离文本为斜体
        if (!this.registryKey.equals(ClientUtils.getWorld().getRegistryKey())) {
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
     * 停止渲染但不播放消失动画
     */
    public void discard() {
        if (this.isDone()) {
            return;
        }
        this.remaining = -Integer.MAX_VALUE;
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

    public final Vec3d getTarget() {
        return this.target;
    }

    public void setTarget(RegistryKey<World> registryKey, Vec3d vec3d) {
        this.target = vec3d;
        this.registryKey = registryKey;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Waypoint that = (Waypoint) o;
        return Objects.equals(icon, that.icon) && Objects.equals(registryKey, that.registryKey) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(icon, target);
    }

    public void requestServerToStop() {
    }

    public abstract String getName();
}
