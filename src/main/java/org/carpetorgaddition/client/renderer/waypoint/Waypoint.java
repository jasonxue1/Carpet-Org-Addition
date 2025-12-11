package org.carpetorgaddition.client.renderer.waypoint;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
    private Vec3 target;
    @NotNull
    protected Vec3 lastTarget;
    /**
     * 路径点所在时间的注册表项
     */
    @NotNull
    protected ResourceKey<Level> registryKey;
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
    public static final Identifier HIGHLIGHT = Identifier.withDefaultNamespace("textures/map/decorations/red_x.png");
    public static final Identifier NAVIGATOR = Identifier.withDefaultNamespace("textures/map/decorations/target_x.png");

    public Waypoint(@NotNull ResourceKey<Level> registryKey, @NotNull Vec3 target, Identifier icon, long duration, boolean persistent) {
        this.registryKey = registryKey;
        this.target = target;
        this.lastTarget = target;
        this.icon = icon;
        this.remaining = duration;
        this.persistent = persistent;
    }

    public Waypoint(Level world, Vec3 target, Identifier icon, long duration, boolean persistent) {
        this(world.dimension(), target, icon, duration, persistent);
    }

    public void render(PoseStack matrixStack, MultiBufferSource consumers, Camera camera, DeltaTracker tickCounter) {
        if (this.isDone()) {
            return;
        }
        Vec3 revised = this.getRevisedPos();
        if (revised == null) {
            return;
        }
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(false);
        if (this.tickDelta > tickDelta) {
            this.tick();
        }
        this.lastTickDelta = this.tickDelta;
        this.tickDelta = tickDelta;
        // 获取摄像机位置
        Vec3 cameraPos = camera.position();
        // 玩家距离目标的位置
        Vec3 offset = revised.subtract(cameraPos);
        // 获取客户端渲染距离
        int renderDistance = ClientUtils.getGameOptions().renderDistance().get() * 16;
        // 修正路径点渲染位置
        Vec3 correction = new Vec3(offset.x(), offset.y(), offset.z());
        if (correction.length() > renderDistance) {
            // 将路径点位置限制在渲染距离内
            correction = correction.normalize().scale(renderDistance);
        }
        matrixStack.pushPose();
        this.transform(matrixStack, camera, correction);
        this.render(matrixStack, consumers);
        // 如果准星正在指向路径点，显示文本
        if (isWatching(camera, revised)) {
            drawDistance(matrixStack, consumers, offset);
        }
        matrixStack.popPose();
    }

    private void render(PoseStack matrixStack, MultiBufferSource consumers) {
        RenderType renderLayer = RenderTypes.fireScreenEffect(this.icon);
        Matrix4f matrix4f = matrixStack.last().pose();
        VertexConsumer vertexConsumer = consumers.getBuffer(renderLayer);
        vertexConsumer.addVertex(matrix4f, -1F, -1F, 0F).setUv(0F, 0F).setColor(-1);
        vertexConsumer.addVertex(matrix4f, -1F, 1F, 0F).setUv(0F, 1F).setColor(-1);
        vertexConsumer.addVertex(matrix4f, 1F, 1F, 0F).setUv(1F, 1F).setColor(-1);
        vertexConsumer.addVertex(matrix4f, 1F, -1F, 0F).setUv(1F, 0F).setColor(-1);
    }

    /**
     * 变换矩阵
     */
    protected void transform(PoseStack matrixStack, Camera camera, Vec3 correction) {
        // 将路径点平移到方块位置
        matrixStack.translate(correction.x(), correction.y(), correction.z());
        float scale = this.getScale(correction.length());
        // 路径点大小
        matrixStack.scale(scale, scale, scale);
        // 翻转路径点
        matrixStack.mulPose(new Quaternionf(-1, 0, 0, 0));
        // 让路径点始终对准玩家
        matrixStack.mulPose(new Quaternionf().rotateY((float) ((Math.PI / 180.0) * (camera.yRot() - 180F))));
        matrixStack.mulPose(new Quaternionf().rotateX((float) ((Math.PI / 180.0) * (-camera.xRot()))));
    }

    protected void tick() {
        this.age++;
        if (!this.persistent || this.remaining <= 0) {
            this.remaining--;
        }
    }

    @Nullable
    protected Vec3 getRevisedPos() {
        // 获取玩家所在维度ID
        ResourceKey<Level> key = ClientUtils.getWorld().dimension();
        // 玩家和路径点在同一维度
        Vec3 interpolation = getInterpolation();
        if (this.registryKey.equals(key)) {
            return interpolation;
        }
        Camera camera = ClientUtils.getCamera();
        // 玩家在主世界，路径点在下界，将路径点坐标换算成主世界坐标
        if (WorldUtils.isOverworld(key) && WorldUtils.isTheNether(this.registryKey)) {
            return new Vec3(interpolation.x() * 8, camera.position().y(), interpolation.z() * 8);
        }
        // 玩家在下界，路径点在主世界，将路径点坐标换算成下界坐标
        if (WorldUtils.isTheNether(key) && WorldUtils.isOverworld(this.registryKey)) {
            return new Vec3(interpolation.x() / 8, camera.position().y(), interpolation.z() / 8);
        }
        return null;
    }

    protected Vec3 getInterpolation() {
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
     * @see EnderMan#isBeingStaredBy(Player)
     */
    @SuppressWarnings("JavadocReference")
    private boolean isWatching(Camera camera, Vec3 target) {
        float f = camera.xRot() * (float) (Math.PI / 180.0);
        float g = -camera.yRot() * (float) (Math.PI / 180.0);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        Vec3 vec3d = new Vec3(i * j, -k, h * j).normalize();
        Vec3 vec3d2 = new Vec3(target.x() - camera.position().x(), target.y() - camera.position().y(), target.z() - camera.position().z());
        double d = vec3d2.length();
        vec3d2 = vec3d2.normalize();
        double e = vec3d.dot(vec3d2);
        return e > 0.999 - (0.025 / d);
    }

    /**
     * 绘制距离文本
     */
    private void drawDistance(PoseStack matrixStack, MultiBufferSource consumers, Vec3 offset) {
        Font textRenderer = ClientUtils.getTextRenderer();
        // 计算距离
        double distance = offset.length();
        String formatted = distance >= 1000 ? "%.1fkm".formatted(distance / 1000) : "%.1fm".formatted(distance);
        TextBuilder builder = new TextBuilder(formatted);
        // 如果玩家与路径点不在同一纬度，设置距离文本为斜体
        if (!this.registryKey.equals(ClientUtils.getWorld().dimension())) {
            builder.setItalic();
        }
        // 获取文本宽度
        int width = textRenderer.width(formatted);
        // 获取背景不透明度
        float backgroundOpacity = ClientUtils.getGameOptions().getBackgroundOpacity(0.25F);
        int opacity = (int) (backgroundOpacity * 255.0F) << 24;
        matrixStack.pushPose();
        // 缩小文字
        matrixStack.scale(0.15F, 0.15F, 0.15F);
        // 渲染文字
        textRenderer.drawInBatch(builder.build(), -width / 2F, 8, CommonColors.WHITE, false,
                matrixStack.last().pose(), consumers,
                Font.DisplayMode.SEE_THROUGH, opacity, 1);
        matrixStack.popPose();
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

    public final Vec3 getTarget() {
        return this.target;
    }

    public void setTarget(ResourceKey<Level> registryKey, Vec3 vec3d) {
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
