package org.carpetorgaddition.client.renderer.path;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.client.renderer.BoxRenderer;
import org.carpetorgaddition.client.renderer.LineRenderer;
import org.carpetorgaddition.client.renderer.WorldRenderer;
import org.carpetorgaddition.client.renderer.substitute.WorldRenderContext;
import org.carpetorgaddition.client.util.ClientUtils;

import java.util.ArrayList;
import java.util.List;

public class PathRenderer implements WorldRenderer {
    private final List<BoxRenderer> boxRenderers = new ArrayList<>();
    private final List<LineRenderer> lineRenderers = new ArrayList<>();
    private final int id;

    public PathRenderer(int id, List<Vec3d> list) {
        this.id = id;
        if (list.isEmpty()) {
            return;
        }
        for (Vec3d vec3d : list) {
            this.boxRenderers.add(new BoxRenderer(new Box(vec3d, vec3d).expand(0.1)));
        }
        for (int i = 0; i < list.size() - 1; i++) {
            this.lineRenderers.add(new LineRenderer(list.get(i), list.get(i + 1)));
        }
    }

    @Override
    public void render(WorldRenderContext context) {
        this.lineRenderers.forEach(renderer -> renderer.render(context));
        this.boxRenderers.forEach(renderer -> renderer.render(context));
    }

    @Override
    public boolean shouldStop() {
        if (this.boxRenderers.isEmpty()) {
            return true;
        }
        ClientWorld world = ClientUtils.getWorld();
        return world.getEntityById(this.id) == null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            return this.id == ((PathRenderer) obj).id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}
