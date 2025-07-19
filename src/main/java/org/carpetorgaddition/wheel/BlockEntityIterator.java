package org.carpetorgaddition.wheel;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.carpetorgaddition.util.MathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class BlockEntityIterator implements Iterable<BlockEntity> {
    private final World world;
    private final BlockIterator blockIterator;
    private final BlockPos from;
    private final BlockPos to;

    public BlockEntityIterator(World world, BlockPos sourcePos, int range) {
        this.world = world;
        int minX = sourcePos.getX() - Math.abs(range);
        int minY = world.getBottomY();
        int minZ = sourcePos.getZ() - Math.abs(range);
        int maxX = sourcePos.getX() + Math.abs(range);
        int maxY = world.getTopY();
        int maxZ = sourcePos.getZ() + Math.abs(range);
        this.from = new BlockPos(minX, minY, minZ);
        this.to = new BlockPos(maxX, maxY, maxZ);
        this.blockIterator = new BlockIterator(this.from, this.to);
    }

    public BlockEntityIterator(World world, BlockPos from, BlockPos to) {
        this.world = world;
        this.from = MathUtils.toMinBlockPos(from, to);
        this.to = MathUtils.toMaxBlockPos(from, to);
        this.blockIterator = new BlockIterator(from, to);
    }

    public static BlockEntityIterator ofAbove(EntityPlayerMPFake fakePlayer, int range) {
        World world = fakePlayer.getWorld();
        BlockPos blockPos = fakePlayer.getBlockPos();
        return ofAbove(world, blockPos, range);
    }

    public static BlockEntityIterator ofAbove(World world, BlockPos blockPos, int range) {
        BlockPos from = new BlockPos(blockPos.getX() - range, blockPos.getY(), blockPos.getZ() - range);
        BlockPos to = new BlockPos(blockPos.getX() + range, world.getTopY(), blockPos.getZ() + range);
        return new BlockEntityIterator(world, from, to);
    }

    public <T extends Entity> boolean contains(Class<T> type) {
        return !this.world.getNonSpectatingEntities(type, this.toBox()).isEmpty();
    }

    @Override
    public @NotNull Iterator<BlockEntity> iterator() {
        return new Itr();
    }

    public Box toBox() {
        return this.blockIterator.toBox();
    }

    private class Itr implements Iterator<BlockEntity> {
        private BlockEntity next;
        private Iterator<BlockEntity> current;
        private boolean alreadyNext = false;
        private final Iterator<WorldChunk> chunkIterator;

        private Itr() {
            this.chunkIterator = createChunkIterator();
        }

        private Iterator<WorldChunk> createChunkIterator() {
            WorldChunk minChunk = world.getWorldChunk(from);
            WorldChunk maxChunk = world.getWorldChunk(to);
            ChunkPos minChunkPos = minChunk.getPos();
            ChunkPos maxChunkPos = maxChunk.getPos();
            return new Iterator<>() {
                private int currentX = minChunkPos.x;
                private int currentZ = minChunkPos.z;

                @Override
                public boolean hasNext() {
                    return this.currentZ <= maxChunkPos.z;
                }

                @Nullable
                @Override
                public WorldChunk next() {
                    if (this.hasNext()) {
                        Chunk chunk = world.getChunk(currentX, currentZ, ChunkStatus.FULL, false);
                        this.currentX++;
                        if (currentX > maxChunkPos.x) {
                            this.currentX = minChunkPos.x;
                            this.currentZ++;
                        }
                        return chunk instanceof WorldChunk worldChunk ? worldChunk : null;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            };
        }

        @Override
        public boolean hasNext() {
            if (this.alreadyNext) {
                return this.next != null;
            }
            this.alreadyNext = true;
            // this.current != null用来在this.chunkIterator.hasNext()耗尽时处理未完成的迭代器
            while (this.current != null || this.chunkIterator.hasNext()) {
                if (this.current == null) {
                    WorldChunk worldChunk = chunkIterator.next();
                    // 区块可能未加载
                    if (worldChunk == null) {
                        continue;
                    }
                    this.current = worldChunk.getBlockEntities().values().iterator();
                }
                while (this.current.hasNext()) {
                    BlockEntity blockEntity = this.current.next();
                    if (blockIterator.contains(blockEntity.getPos())) {
                        this.next = blockEntity;
                        return true;
                    }
                }
                this.current = null;
            }
            return false;
        }

        @Override
        public BlockEntity next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.alreadyNext = false;
            BlockEntity blockEntity = this.next;
            this.next = null;
            return blockEntity;
        }
    }
}
