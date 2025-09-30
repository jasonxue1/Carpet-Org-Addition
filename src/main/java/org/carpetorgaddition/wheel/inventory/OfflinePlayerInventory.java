package org.carpetorgaddition.wheel.inventory;

import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import net.minecraft.util.WorldSavePath;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.wheel.GameProfileCache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class OfflinePlayerInventory extends AbstractCustomSizeInventory {
    protected final FabricPlayerAccessor accessor;
    /**
     * 是否在打开物品栏时在日志输出打开物品栏的玩家
     */
    private boolean showLog = true;

    public OfflinePlayerInventory(FabricPlayerAccessor accessor) {
        this.accessor = accessor;
    }

    /**
     * 根据玩家名称获取玩家档案
     */
    public static Optional<GameProfile> getGameProfile(String username, MinecraftServer server) {
        try {
            Optional<GameProfile> optional = GameProfileCache.getGameProfile(username);
            if (optional.isPresent()) {
                return optional;
            }
            // 获取玩家的离线UUID
            UUID uuid = Uuids.getOfflinePlayerUuid(username);
            if (playerDataExists(uuid, server)) {
                GameProfile gameProfile = new GameProfile(uuid, username);
                GameProfileCache.put(gameProfile);
                return Optional.of(gameProfile);
            }
        } catch (JsonParseException | NullPointerException e) {
            // 译：读取usercache.json时出现意外问题，正在使用离线玩家UUID
            CarpetOrgAddition.LOGGER.warn("An unexpected issue occurred while reading usercache.json, using offline player UUID", e);
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
            PlayerManager playerManager = server.getPlayerManager();
            if (playerManager.isWhitelisted(gameProfile) || playerManager.isOperator(gameProfile)) {
                throw CommandUtils.createException("carpet.commands.player.inventory.offline.permission");
            }
        }
    }

    public static Optional<PlayerConfigEntry> getPlayerConfigEntry(UUID uuid, MinecraftServer server) {
        if (playerDataExists(uuid, server)) {
            Optional<PlayerConfigEntry> optional = GameProfileCache.getPlayerConfigEntry(uuid);
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
    public void onOpen(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            this.accessor.onOpen((ServerPlayerEntity) player);
            if (this.showLog) {
                // 译：{}打开了离线玩家{}的物品栏
                CarpetOrgAddition.LOGGER.info(
                        "{} opened the inventory of the offline player {}.",
                        FetcherUtils.getPlayerName(player),
                        this.accessor.getGameProfile().getName()
                );
            }
        }
    }

    @Override
    public void onClose(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            this.accessor.onClose((ServerPlayerEntity) player);
        }
    }

    @Override
    protected Inventory getInventory() {
        return this.accessor.getInventory();
    }

    public void setShowLog(boolean showLog) {
        this.showLog = showLog;
    }
}
