package org.carpetorgaddition.wheel.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.ReadView;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.mixin.accessor.PlayerManagerAccessor;
import org.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.wheel.UuidNameMappingTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OfflinePlayerInventory extends AbstractCustomSizeInventory {
    /**
     * 正在操作物品栏的玩家，键表示被打开物品栏玩家的配置文件，值表示正在打开物品栏的玩家
     */
    public static final Map<PlayerProfile, ServerPlayerEntity> INVENTORY_OPERATOR_PLAYERS = new ConcurrentHashMap<>();
    private final PlayerProfile profile;
    protected final FakePlayer fabricPlayer;

    public OfflinePlayerInventory(MinecraftServer server, GameProfile gameProfile) {
        this.fabricPlayer = FakePlayer.get(server.getOverworld(), gameProfile);
        this.profile = new PlayerProfile(gameProfile);
    }

    /**
     * @see PlayerManager#onPlayerConnect(ClientConnection, ServerPlayerEntity, ConnectedClientData)
     */
    @SuppressWarnings("deprecation")
    private void initFakePlayer(MinecraftServer server) {
        ErrorReporter.Logging logging = new ErrorReporter.Logging(CarpetOrgAddition.LOGGER);
        try (logging) {
            Optional<ReadView> optional = server.getPlayerManager()
                    .loadPlayerData(this.fabricPlayer.getPlayerConfigEntry())
                    .map(nbtCompound -> NbtReadView.create(logging, server.getRegistryManager(), nbtCompound));
            ServerPlayerEntity.SavePos pos = optional
                    .flatMap(nbt -> nbt.read(ServerPlayerEntity.SavePos.CODEC))
                    .orElse(ServerPlayerEntity.SavePos.EMPTY);
            optional.ifPresent(this.fabricPlayer::readData);
            ServerWorld world = server.getWorld(pos.dimension().orElse(World.OVERWORLD));
            if (world != null) {
                // 设置玩家所在维度
                this.fabricPlayer.setServerWorld(world);
            }
        }
    }

    /**
     * 根据玩家名称获取玩家档案
     *
     * @apiNote {@link UserCache#findByName(String)}似乎不是只读的
     */
    public static Optional<GameProfile> getGameProfile(String username, boolean caseSensitive, MinecraftServer server) {
        try {
            // 从本地获取玩家的在线UUID
            JsonArray array = IOUtils.loadJson(IOUtils.USERCACHE_JSON, JsonArray.class);
            Optional<GameProfile> entry = readUsercacheJson(username, caseSensitive, array, server);
            if (entry.isPresent()) {
                return entry;
            }
        } catch (IOException | JsonParseException | NullPointerException e) {
            // 译：读取usercache.json时出现意外问题，正在使用离线玩家UUID
            CarpetOrgAddition.LOGGER.warn("An unexpected issue occurred while reading usercache.json, using offline player UUID", e);
        }
        Optional<GameProfile> optional = UuidNameMappingTable.getInstance().getGameProfile(username, caseSensitive);
        if (optional.isPresent()) {
            return optional;
        }
        // 获取玩家的离线UUID
        UUID uuid = Uuids.getOfflinePlayerUuid(username);
        if (playerDataExists(uuid, server)) {
            return Optional.of(new GameProfile(uuid, username));
        }
        return Optional.empty();
    }

    /**
     * 从usercache.json读取玩家档案
     *
     * @param username 玩家的名称
     * @param array    usercache.json保存的json数组
     * @return 玩家档案，可能为空
     */
    private static Optional<GameProfile> readUsercacheJson(String username, boolean caseSensitive, JsonArray array, MinecraftServer server) {
        Set<Map.Entry<String, String>> set = array.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .map(json -> Map.entry(json.get("name").getAsString(), json.get("uuid").getAsString()))
                .collect(Collectors.toSet());
        for (Map.Entry<String, String> entry : set) {
            String key = entry.getKey();
            if (caseSensitive ? Objects.equals(username, key) : username.equalsIgnoreCase(key)) {
                UUID uuid = UUID.fromString(entry.getValue());
                if (playerDataExists(uuid, server)) {
                    return Optional.of(new GameProfile(uuid, key));
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /**
     * 检查玩家是否有权限打开离线玩家物品栏
     */
    public static void checkPermission(MinecraftServer server, GameProfile gameProfile, ServerPlayerEntity player) throws CommandSyntaxException {
        if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.get().permissionRequired()) {
            if (player.hasPermissionLevel(2)) {
                return;
            }
            PlayerProfile profile = new PlayerProfile(gameProfile);
            if (isWhitelistPlayer(server, profile) || isOperatorPlayer(server, profile)) {
                throw CommandUtils.createException("carpet.commands.player.inventory.offline.permission");
            }
        }
    }

    private static boolean isWhitelistPlayer(MinecraftServer server, PlayerProfile profile) {
        PlayerManager playerManager = server.getPlayerManager();
        JsonArray array;
        try {
            array = IOUtils.loadJson(playerManager.getWhitelist().getFile(), JsonArray.class);
        } catch (IOException e) {
            return false;
        }
        return contains(profile, array);
    }

    private static boolean isOperatorPlayer(MinecraftServer server, PlayerProfile profile) {
        PlayerManager playerManager = server.getPlayerManager();
        JsonArray array;
        try {
            array = IOUtils.loadJson(playerManager.getOpList().getFile(), JsonArray.class);
        } catch (IOException e) {
            return false;
        }
        return contains(profile, array);
    }

    private static boolean contains(PlayerProfile profile, JsonArray array) {
        // 原版方法似乎要求玩家名称和玩家UUID都要匹配
        Set<PlayerProfile> profiles = array.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .map(json -> Map.entry(json.get("uuid").getAsString(), json.get("name").getAsString()))
                .map(entry -> new PlayerProfile(new GameProfile(UUID.fromString(entry.getKey()), entry.getValue())))
                .collect(Collectors.toSet());
        return profiles.contains(profile);
    }

    public static Optional<PlayerConfigEntry> getPlayerConfigEntry(UUID uuid, MinecraftServer server) {
        if (playerDataExists(uuid, server)) {
            NameToIdCache userCache = server.getApiServices().nameToIdCache();
            if (userCache != null) {
                Optional<PlayerConfigEntry> optional = userCache.getByUuid(uuid);
                if (optional.isPresent()) {
                    return optional;
                }
            }
            Optional<PlayerConfigEntry> optional = UuidNameMappingTable.getInstance().getGameProfile(uuid);
            return Optional.of(optional.orElse(new PlayerConfigEntry(new GameProfile(uuid, OfflinePlayerSearchTask.UNKNOWN))));
        }
        return Optional.empty();
    }

    /**
     * @return 玩家数据是否存在
     */
    public static boolean playerDataExists(UUID uuid, MinecraftServer server) {
        String filename = uuid.toString() + ".dat";
        Path path = server.getSavePath(WorldSavePath.PLAYERDATA).resolve(filename);
        return Files.exists(path);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onOpen(ContainerUser user) {
        LivingEntity livingEntity = user.asLivingEntity();
        MinecraftServer server = livingEntity.getServer();
        if (server != null && user instanceof ServerPlayerEntity player) {
            INVENTORY_OPERATOR_PLAYERS.put(this.profile, player);
            this.initFakePlayer(server);
            // 译：{}打开了离线玩家{}的物品栏
            CarpetOrgAddition.LOGGER.info(
                    "{} opened the inventory of the offline player {}.",
                    livingEntity.getName().getString(),
                    this.profile.name
            );
        }
    }

    @Override
    public void onClose(ContainerUser user) {
        try {
            LivingEntity livingEntity = user.asLivingEntity();
            // 保存玩家数据
            MinecraftServer server = livingEntity.getServer();
            if (server != null) {
                PlayerManager playerManager = server.getPlayerManager();
                PlayerManagerAccessor accessor = (PlayerManagerAccessor) playerManager;
                accessor.savePlayerEntityData(this.fabricPlayer);
            }
        } finally {
            INVENTORY_OPERATOR_PLAYERS.remove(this.profile);
        }
    }

    @Override
    protected Inventory getInventory() {
        return this.fabricPlayer.getInventory();
    }

    public static class PlayerProfile {
        private final String name;
        private final UUID uuid;

        public PlayerProfile(GameProfile gameProfile) {
            this.name = gameProfile.name();
            this.uuid = gameProfile.id();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (this.getClass() == obj.getClass()) {
                PlayerProfile other = (PlayerProfile) obj;
                // 玩家名称和玩家UUID有一个匹配成功就视为相同
                String thisLowerCase = this.name.toLowerCase(Locale.ROOT);
                String otherLowerCase = other.name.toLowerCase(Locale.ROOT);
                return Objects.equals(thisLowerCase, otherLowerCase) || Objects.equals(this.uuid, other.uuid);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.uuid.hashCode();
        }
    }
}
