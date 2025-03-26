package org.carpetorgaddition.util.wheel;

import com.mojang.authlib.GameProfile;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UUIDNameMappingTable {
    private final HashMap<UUID, String> hashMap = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final UUIDNameMappingTable MAPPING_TABLE = new UUIDNameMappingTable();
    private final File config = IOUtils.createConfigFile("uuid_name_mapping.txt");

    private UUIDNameMappingTable() {
    }

    public static UUIDNameMappingTable getInstance() {
        return MAPPING_TABLE;
    }

    public Optional<String> get(UUID uuid) {
        try {
            this.lock.readLock().lock();
            String value = this.hashMap.get(uuid);
            return Optional.ofNullable(value);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void put(GameProfile gameProfile) {
        this.put(gameProfile.getId(), gameProfile.getName());
    }

    public void put(UUID uuid, String name) {
        try {
            this.lock.writeLock().lock();
            this.hashMap.put(uuid, name);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public void init() {
        try {
            IOUtils.createFileIfNotExists(this.config);
            BufferedReader reader = IOUtils.toReader(this.config);
            try (reader) {
                this.lock.writeLock().lock();
                this.loadFromFile(reader);
            } finally {
                this.lock.writeLock().unlock();
            }
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("无法从文件读取玩家UUID与名称的映射表", e);
        }
    }

    private void loadFromFile(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            int index = line.indexOf('=');
            if (index == -1) {
                continue;
            }
            UUID uuid;
            String uuidString = line.substring(0, index).strip();
            try {
                uuid = UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                continue;
            }
            String playerName = line.substring(index).strip();
            if (playerName.isEmpty()) {
                continue;
            }
            this.hashMap.put(uuid, playerName);
        }
    }

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
