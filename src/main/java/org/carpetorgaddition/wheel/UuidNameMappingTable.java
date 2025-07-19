package org.carpetorgaddition.wheel;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * 使用{@code /finder item 物品ID from offline_player}命令查找离线玩家的物品时，
 * 玩家数据是以UUID的形式保存的，但是召唤假玩家需要的是玩家名，因此就需要根据玩家UUID
 * 找到对应的玩家名。
 * </p>
 * <p>
 * 在游戏根目录下，有一个{@code usercache.json}文件，虽然可以通过它来获取对应的玩家名称，
 * 但是，这个文件只会保存1000名玩家，长时间没有上线的玩家可能会被覆盖，因此需要一个类来记录
 * 所有的映射关系，保证不常上线的玩家名也能获取。
 * </p>
 * <p>
 * Mojang提供了根据玩家UUID获取玩家名称的API，但是这对服务器中的非正版玩家无能为力，
 * 并且如果找不到名称的玩家狠多，如果每个都去查询，Mojang也不会同时处理这么多的请求。
 * </p>
 * <p>
 * 本类用于记录每个上线过的玩家UUID与玩家名称的对应关系，它在所有世界中共享，但是并不能彻底解决以上问题：
 * <ul>
 * <li>只能记录安装本Mod之后上线的玩家，不能记录之前的。</li>
 * <li>玩家可能更改他们的名称。</li>
 * </ul>
 * </p>
 * <p>
 * 综上所述，本类不是{@code usercache.json}的替代，而是该文件中找不到玩家名时的备用方案。
 * </p>
 *
 * @see <a href="https://zh.minecraft.wiki/w/玩家档案缓存存储格式">玩家档案缓存存储格式</a>
 */
public class UuidNameMappingTable {
    private final HashMap<UUID, String> hashMap = new HashMap<>();
    /**
     * 集合可能被多个线程同时访问
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final File config = IOUtils.configFile("uuid_name_mapping.txt", true);
    private static final UuidNameMappingTable MAPPING_TABLE = new UuidNameMappingTable();
    /**
     * Mojang提供的根据玩家UUID查询玩家名的API
     */
    public static final String MOJANG_API = "https://api.minecraftservices.com/minecraft/profile/lookup/%s";

    private UuidNameMappingTable() {
    }

    public static UuidNameMappingTable getInstance() {
        return MAPPING_TABLE;
    }

    /**
     * 根据UUID获取对应的玩家名
     */
    public Optional<String> get(UUID uuid) {
        try {
            this.lock.readLock().lock();
            String value = this.hashMap.get(uuid);
            return Optional.ofNullable(value);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Optional<GameProfile> getGameProfile(UUID uuid) {
        Optional<String> optional = get(uuid);
        return optional.map(name -> new GameProfile(uuid, name));
    }

    /**
     * 根据玩家名称获取玩家UUID
     *
     * @param name          玩家的名称
     * @param caseSensitive 是否区分大小写
     */
    public Optional<GameProfile> getGameProfile(@NotNull String name, boolean caseSensitive) {
        try {
            this.lock.readLock().lock();
            Set<Map.Entry<UUID, String>> entries = this.hashMap.entrySet();
            for (Map.Entry<UUID, String> entry : entries) {
                String value = entry.getValue();
                boolean equal = caseSensitive ? name.equals(value) : name.equalsIgnoreCase(value);
                if (equal) {
                    return Optional.of(new GameProfile(entry.getKey(), value));
                }
            }
            return Optional.empty();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public Optional<GameProfile> fetchGameProfileWithBackup(UserCache userCache, UUID uuid) {
        Optional<GameProfile> optional = userCache.getByUuid(uuid);
        if (optional.isPresent()) {
            return optional;
        }
        return this.getGameProfile(uuid);
    }

    public void put(GameProfile gameProfile) {
        this.put(gameProfile.getId(), gameProfile.getName());
    }

    /**
     * 将玩家UUID与玩家名称的对应关系添加至集合
     */
    public void put(UUID uuid, String name) {
        try {
            this.lock.writeLock().lock();
            this.hashMap.put(uuid, name);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * 从文件加载玩家UUID与名称映射
     */
    public void init() {
        try {
            IOUtils.createFileIfNotExists(this.config);
            BufferedReader reader = IOUtils.toReader(this.config);
            try (reader) {
                this.loadFromFile(reader);
            }
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("无法从文件读取玩家UUID与名称的映射表", e);
        }
    }

    private void loadFromFile(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String[] split = line.split("=");
            if (split.length != 2) {
                continue;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(split[0].strip());
            } catch (IllegalArgumentException e) {
                continue;
            }
            String playerName = split[1].strip();
            if (playerName.isEmpty()) {
                continue;
            }
            try {
                this.lock.writeLock().lock();
                this.hashMap.put(uuid, playerName);
            } finally {
                this.lock.writeLock().unlock();
            }
        }
    }

    /**
     * 将玩家UUID与名称的映射表写入本地文件
     */
    public void save() {
        try {
            BufferedWriter writer = IOUtils.toWriter(this.config);
            try (writer) {
                this.lock.readLock().lock();
                Set<Map.Entry<UUID, String>> entries = this.hashMap.entrySet();
                for (Map.Entry<UUID, String> entry : entries) {
                    writer.write(entry.getKey().toString() + "=" + entry.getValue());
                    writer.newLine();
                }
            } finally {
                this.lock.readLock().unlock();
            }
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("无法将玩家UUID与名称的映射表写入文件", e);
        }
    }
}
