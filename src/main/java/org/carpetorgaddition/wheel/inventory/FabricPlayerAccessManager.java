package org.carpetorgaddition.wheel.inventory;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FabricPlayerAccessManager {
    private final MinecraftServer server;
    /**
     * 正在操作物品栏的玩家，键表示被打开物品栏玩家的配置文件，值表示正在打开物品栏的玩家集合
     */
    private final Map<PlayerConfigEntry, Set<ServerPlayerEntity>> viewers = new ConcurrentHashMap<>();
    /**
     * 配置文件对应的Fabric玩家访问器
     */
    private final Map<PlayerConfigEntry, FabricPlayerAccessor> accessors = new ConcurrentHashMap<>();

    public FabricPlayerAccessManager(MinecraftServer server) {
        this.server = server;
    }

    public FabricPlayerAccessor getOrCreate(GameProfile gameProfile) {
        return getOrCreate(new PlayerConfigEntry(gameProfile));
    }

    public FabricPlayerAccessor getOrCreate(PlayerConfigEntry entry) {
        return this.accessors.computeIfAbsent(entry, profile -> new FabricPlayerAccessor(this.server, profile, this));
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
    public Set<ServerPlayerEntity> getViewers(PlayerConfigEntry entry) {
        Set<ServerPlayerEntity> set = this.viewers.get(entry);
        return set == null ? Set.of() : set;
    }

    public void addViewers(PlayerConfigEntry entry, Set<ServerPlayerEntity> viewers) {
        this.viewers.put(entry, viewers);
    }

    public void removeAccessor(PlayerConfigEntry entry) {
        this.accessors.remove(entry);
    }

    public void removeViewer(PlayerConfigEntry entry) {
        this.viewers.remove(entry);
    }
}
