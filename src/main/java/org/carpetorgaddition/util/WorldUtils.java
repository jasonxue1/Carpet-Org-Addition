package org.carpetorgaddition.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class WorldUtils {
    private WorldUtils() {
    }

    public static final String OVERWORLD = "minecraft:overworld";
    public static final String THE_NETHER = "minecraft:the_nether";
    public static final String THE_END = "minecraft:the_end";
    public static final String SIMPLE_OVERWORLD = "overworld";
    public static final String SIMPLE_THE_NETHER = "the_nether";
    public static final String SIMPLE_THE_END = "the_end";

    /**
     * 获取区域内所有方块坐标的集合
     *
     * @param box 用来指定的区域盒子对象
     * @return 盒子内所有的方块坐标
     * @see BlockPosTraverser
     * @deprecated 如果Box对象的范围比较大，则会将这个范围内的所有方块坐标对象全部返回，
     * 这对内存是一个较大的负担。例如：如果Box的范围是长宽高各256，那么将有256*256*256=16777216个对象被创建，
     * 并且在集合对象使用完毕之前不会被回收，短时间内多次调用时，容易导致{@link OutOfMemoryError}
     */
    @Deprecated(forRemoval = true)
    public static ArrayList<BlockPos> allBlockPos(AABB box) {
        int endX = (int) box.maxX;
        int endY = (int) box.maxY;
        int endZ = (int) box.maxZ;
        ArrayList<BlockPos> list = new ArrayList<>();
        for (int startX = (int) box.minX; startX < endX; startX++) {
            for (int startY = (int) box.minY; startY < endY; startY++) {
                for (int startZ = (int) box.minZ; startZ < endZ; startZ++) {
                    list.add(new BlockPos(startX, startY, startZ));
                }
            }
        }
        return list;
    }

    /**
     * 根据维度获取对应的颜色
     *
     * @return 如果是下界，返回红色；如果是末地，返回紫色；如果是主世界或者自定义维度，返回绿色
     */
    public static ChatFormatting getColor(Level world) {
        return switch (getDimensionId(world)) {
            case THE_NETHER -> ChatFormatting.RED;
            case THE_END -> ChatFormatting.DARK_PURPLE;
            default -> ChatFormatting.GREEN;
        };
    }

    /**
     * 获取方块坐标的字符串形式
     *
     * @param blockPos 要转换为字符串形式的方块位置对象
     * @return 方块坐标的字符串形式
     */
    public static String toPosString(BlockPos blockPos) {
        return blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ();
    }

    /**
     * 将方块坐标转换为字符串形式，坐标前追加维度id
     */
    public static String toWorldPosString(Level world, BlockPos blockPos) {
        return getDimensionId(world) + "[" + toPosString(blockPos) + "]";
    }

    /**
     * 获取当前维度的ID
     *
     * @param world 当前世界的对象
     * @return 当前维度的ID
     */
    public static String getDimensionId(Level world) {
        return world.dimension().identifier().toString();
    }

    /**
     * 根据维度获取世界对象
     *
     * @param server    游戏当前的服务器
     * @param dimension 一个维度的id
     */
    public static ServerLevel getWorld(MinecraftServer server, String dimension) {
        String[] split = dimension.split(":");
        Identifier identifier;
        if (split.length == 1) {
            identifier = Identifier.fromNamespaceAndPath("minecraft", dimension);
        } else if (split.length == 2) {
            identifier = Identifier.fromNamespaceAndPath(split[0], split[1]);
        } else {
            throw new IllegalArgumentException();
        }
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, identifier));
    }

    /**
     * 将字符串解析为世界ID
     *
     * @param worldId 世界的ID，如果指定了命名空间，则使用指定的，否则使用minecraft
     * @return 世界类型的注册表项
     */
    public static ResourceKey<Level> getWorld(String worldId) {
        if (worldId.contains(":")) {
            String[] split = worldId.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException();
            }
            return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(split[0], split[1]));
        }
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(worldId));
    }

    /**
     * 从服务器寻找一个指定UUID的实体
     */
    @Nullable
    public static Entity getEntityFromUUID(MinecraftServer server, UUID uuid) {
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity == null) {
                continue;
            }
            return entity;
        }
        return null;
    }

    /**
     * 在指定位置播放一个音效
     */
    public static void playSound(Level world, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundCategory) {
        world.playSound(null, blockPos, soundEvent, soundCategory, 1F, 1F);
    }

    /**
     * 在指定玩家位置播放一个音效
     *
     * @param soundEvent    声音时间
     * @param soundCategory 声音类别
     */
    public static void playSound(ServerPlayer player, SoundEvent soundEvent, SoundSource soundCategory) {
        Level world = FetcherUtils.getWorld(player);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, soundCategory, 1F, 1F);
    }

    /**
     * @return 两个世界的坐标是否可以互相转换
     */
    public static boolean canMappingPos(Level world1, Level world2) {
        return (isOverworld(world1) && isTheNether(world2)) || (isOverworld(world2) && isTheNether(world1));
    }

    /**
     * @return 最大建筑高度
     */
    public static int getMaxArchitectureAltitude(Level world) {
        return world.getMaxY();
    }

    /**
     * @return 最小建筑高度
     */
    public static int getMinArchitectureAltitude(Level world) {
        return world.getMinY();
    }

    /**
     * @return 维度ID是否表示主世界
     */
    public static boolean isOverworld(Level world) {
        return world.dimension() == Level.OVERWORLD;
    }

    /**
     * @return 维度ID是否表示下界
     */
    public static boolean isTheNether(Level world) {
        return world.dimension() == Level.NETHER;
    }

    /**
     * @return 维度ID是否表示末地
     */
    @SuppressWarnings("unused")
    public static boolean isTheEnd(Level world) {
        return world.dimension() == Level.END;
    }

    /**
     * 将一个实体传送到目标维度的指定位置
     *
     * @param source 要传送的实体
     * @param world  要传送到目标维度
     * @param x      目的地X坐标
     * @param y      目的地Y坐标
     * @param z      目的地Z坐标
     * @param yaw    传送后实体的偏航角
     * @param pitch  传送后实体的俯仰角
     */
    public static void teleport(Entity source, ServerLevel world, double x, double y, double z, float yaw, float pitch) {
        source.teleportTo(world, x, y, z, Set.of(), yaw, pitch, true);
    }

    /**
     * 将指定实体传送到目标实体位置
     *
     * @param source 要传送的实体
     * @param target 目标实体
     */
    public static void teleport(Entity source, Entity target) {
        // 不要在客户端传送实体
        if (target.level() instanceof ServerLevel world) {
            teleport(source, world, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        }
    }

    public static boolean isOverworld(ResourceKey<Level> key) {
        return Level.OVERWORLD.equals(key);
    }

    public static boolean isTheNether(ResourceKey<Level> key) {
        return Level.NETHER.equals(key);
    }
}
