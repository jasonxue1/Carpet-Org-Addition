package boat.carpetorgaddition.wheel.inventory;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FabricPlayerAccessManager {
    private final MinecraftServer server;
    /**
     * 正在操作物品栏的玩家，键表示被打开物品栏玩家的配置文件，值表示正在打开物品栏的玩家集合
     */
    private final Map<NameAndId, Set<ServerPlayer>> viewers = new ConcurrentHashMap<>();
    /**
     * 配置文件对应的Fabric玩家访问器
     */
    private final Map<NameAndId, FabricPlayerAccessor> accessors = new ConcurrentHashMap<>();

    public FabricPlayerAccessManager(MinecraftServer server) {
        this.server = server;
    }

    public FabricPlayerAccessor getOrCreate(GameProfile gameProfile) {
        return getOrCreate(new NameAndId(gameProfile));
    }

    public FabricPlayerAccessor getOrCreate(NameAndId entry) {
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
    public Set<ServerPlayer> getViewers(NameAndId entry) {
        Set<ServerPlayer> set = this.viewers.get(entry);
        return set == null ? Set.of() : set;
    }

    public void addViewers(NameAndId entry, Set<ServerPlayer> viewers) {
        this.viewers.put(entry, viewers);
    }

    public void removeAccessor(NameAndId entry) {
        this.accessors.remove(entry);
    }

    public void removeViewer(NameAndId entry) {
        this.viewers.remove(entry);
    }
}
