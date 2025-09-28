package org.carpetorgaddition.wheel.inventory;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FabricPlayerAccessManager {
    private final MinecraftServer server;
    /**
     * 正在操作物品栏的玩家，键表示被打开物品栏玩家的配置文件，值表示正在打开物品栏的玩家集合
     */
    private final Map<GameProfile, Set<ServerPlayerEntity>> viewers = new HashMap<>();
    /**
     * 配置文件对应的Fabric玩家访问器
     */
    private final Map<GameProfile, FabricPlayerAccessor> accessors = new HashMap<>();

    public FabricPlayerAccessManager(MinecraftServer server) {
        this.server = server;
    }

    public FabricPlayerAccessor getOrCreate(GameProfile gameProfile) {
        return this.accessors.computeIfAbsent(gameProfile, profile -> new FabricPlayerAccessor(this.server, profile, this));
    }

    /**
     * @return 是否有玩家正在查看离线玩家物品栏
     */
    public boolean hasViewers() {
        return !this.viewers.isEmpty();
    }

    /**
     * 获取所有正在查看指定离线玩家物品栏的玩家
     */
    public Set<ServerPlayerEntity> getViewers(GameProfile gameProfile) {
        Set<ServerPlayerEntity> set = this.viewers.get(gameProfile);
        return set == null ? Set.of() : set;
    }

    public void addViewers(GameProfile gameProfile, Set<ServerPlayerEntity> viewers) {
        this.viewers.put(gameProfile, viewers);
    }

    public void removeAccessor(GameProfile gameProfile) {
        this.accessors.remove(gameProfile);
    }

    public void removeViewer(GameProfile gameProfile) {
        this.viewers.remove(gameProfile);
    }
}
