package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.wheel.FakePlayerCreateContext;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class GenericUtils {
    /**
     * 当假玩家正在生成时，执行此函数
     */
    public static final ThreadLocal<Consumer<EntityPlayerMPFake>> FAKE_PLAYER_SPAWNING = new ThreadLocal<>();
    /**
     * {@link EntityPlayerMPFake#createFake(String, MinecraftServer, Vec3, double, double, ResourceKey, GameType, boolean)}内部的lambda表达式执行时，调用次函数
     */
    public static final ThreadLocal<Consumer<EntityPlayerMPFake>> INTERNAL_FAKE_PLAYER_SPAWNING = new ThreadLocal<>();
    /**
     * 当前{@code Minecraft}的NBT数据版本
     */
    public static final int CURRENT_DATA_VERSION = getNbtDataVersion();

    private GenericUtils() {
    }

    /**
     * 根据UUID获取实体
     */
    public static Optional<Entity> getEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    /**
     * 根据UUID获取玩家
     */
    public static Optional<ServerPlayer> getPlayer(MinecraftServer server, UUID uuid) {
        return Optional.ofNullable(server.getPlayerList().getPlayer(uuid));
    }

    /**
     * 根据名称获取玩家
     */
    public static Optional<ServerPlayer> getPlayer(MinecraftServer server, String name) {
        return Optional.ofNullable(server.getPlayerList().getPlayerByName(name));
    }

    public static Optional<ServerPlayer> getPlayer(MinecraftServer server, GameProfile gameProfile) {
        return getPlayer(server, gameProfile.name());
    }

    public static Identifier getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static Identifier getId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    public static Optional<Identifier> getId(Level world, Enchantment enchantment) {
        Holder<Enchantment> entry = Holder.direct(enchantment);
        entry.unwrapKey().map(ResourceKey::identifier);
        return getId(world.registryAccess(), enchantment);
    }

    public static Optional<Identifier> getId(MinecraftServer server, Enchantment enchantment) {
        return getId(server.registryAccess(), enchantment);
    }

    public static Optional<Identifier> getId(RegistryAccess registryManager, Enchantment enchantment) {
        Optional<Registry<Enchantment>> optional = registryManager.lookup(Registries.ENCHANTMENT);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        Registry<Enchantment> enchantments = optional.get();
        return Optional.ofNullable(enchantments.getKey(enchantment));
    }


    public static String getIdAsString(Item item) {
        return getId(item).toString();
    }


    public static String getIdAsString(Block block) {
        return getId(block).toString();
    }

    /**
     * 将字符串ID转换为物品
     */
    public static Item getItem(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    /**
     * 将字符串ID转换为方块
     */
    public static Block getBlock(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    public static ResourceKey<Level> getWorld(String worldId) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(worldId));
    }

    /**
     * 创建一个假玩家
     */
    public static void createFakePlayer(String username, MinecraftServer server, FakePlayerCreateContext context) {
        createFakePlayer(username, server, context.pos(), context.yaw(), context.pitch(), context.dimension(), context.gamemode(), context.flying(), context.consumer());
    }

    public static Optional<UUID> uuidFromString(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(str));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Identifier ofIdentifier(String id) {
        return Identifier.fromNamespaceAndPath(CarpetOrgAddition.MOD_ID, id);
    }

    /**
     * 创建一个假玩家
     *
     * @param consumer 玩家生成时执行的函数
     */
    public static void createFakePlayer(String username, MinecraftServer server, Vec3 pos, double yaw, double pitch, ResourceKey<Level> dimension, GameType gamemode, boolean flying, Consumer<EntityPlayerMPFake> consumer) {
        try {
            FAKE_PLAYER_SPAWNING.set(consumer);
            EntityPlayerMPFake.createFake(username, server, pos, yaw, pitch, dimension, gamemode, flying);
        } finally {
            FAKE_PLAYER_SPAWNING.remove();
        }
    }

    /**
     * @return 获取异常的类名+消息形式，如果没有消息，返回异常类的简单类名
     * @apiNote 不使用 {@code toString} 方法是因为方法可能被子类重写
     */
    public static String getExceptionString(Throwable throwable) {
        String name = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        return message == null ? name : name + ": " + message;
    }

    /**
     * @return 当前游戏的NBT数据版本
     */
    public static int getNbtDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    /**
     * 一个占位符，什么也不做
     */
    public static void pass(Object... ignored) {
    }
}
