package org.carpetorgaddition.client.renderer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class WorldRendererManager {
    private static final HashMap<Class<? extends WorldRenderer>, Map<Object, WorldRenderer>> renders = new HashMap<>();

    static {
        // 断开连接时清除路径点
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> renders.clear());
        // 清除不再需要的渲染器
        WorldRenderEvents.START.register(context -> renders.forEach((clazz, renderers) -> renderers.values().removeIf(WorldRenderer::shouldStop)));
    }

    public static void addOrUpdate(WorldRenderer renderer) {
        Map<Object, WorldRenderer> map = renders.computeIfAbsent(renderer.getClass(), k -> new HashMap<>());
        WorldRenderer oldRenderer = map.put(renderer.getKey(), renderer);
        if (oldRenderer != null && oldRenderer.onUpdate(renderer)) {
            map.put(new Object(), oldRenderer);
        }
    }

    public static <T extends WorldRenderer> List<T> getRenderer(Class<T> clazz) {
        Map<Object, WorldRenderer> map = renders.get(clazz);
        if (map == null) {
            return List.of();
        }
        // 返回list，不要直接操作set集合
        return map.values().stream().filter(clazz::isInstance).map(clazz::cast).toList();
    }

    /**
     * 获取所有匹配的渲染器
     */
    public static <T extends WorldRenderer> List<T> getRenderer(Class<T> clazz, Function<T, Boolean> function) {
        return getRenderer(clazz).stream().filter(function::apply).toList();
    }

    /**
     * 获取唯一的渲染器
     */
    @Nullable
    @Deprecated(forRemoval = true)
    public static <T extends WorldRenderer> T getOnlyRenderer(Class<T> clazz, Function<T, Boolean> function) {
        List<T> list = getRenderer(clazz, function);
        // 渲染器应该是唯一的
        if (list.size() > 1) {
            throw new IllegalStateException();
        }
        return list.isEmpty() ? null : list.getFirst();
    }

    @Deprecated(forRemoval = true)
    public static <T extends WorldRenderer> T getOrCreate(Class<T> clazz, Function<T, Boolean> function, Supplier<T> supplier) {
        T onlyRenderer = getOnlyRenderer(clazz, function);
        if (onlyRenderer == null) {
            T renderer = supplier.get();
            addOrUpdate(renderer);
            return renderer;
        }
        return onlyRenderer;
    }

    public static <T extends WorldRenderer> void remove(Class<T> clazz) {
        renders.remove(clazz);
    }

    public static <T extends WorldRenderer> void remove(Class<T> clazz, Function<T, Boolean> function) {
        Map<Object, WorldRenderer> set = renders.get(clazz);
        if (set == null) {
            return;
        }
        set.values().removeIf(renderer -> clazz.isInstance(renderer) ? function.apply(clazz.cast(renderer)) : false);
    }
}
