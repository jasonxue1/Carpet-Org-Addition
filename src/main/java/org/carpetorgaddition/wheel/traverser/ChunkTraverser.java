package org.carpetorgaddition.wheel.traverser;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public class ChunkTraverser extends WorldTraverser<Optional<Chunk>> {
    private final World world;

    public ChunkTraverser(World world, BlockPos from, BlockPos to) {
        super(from, to);
        this.world = world;
    }

    @Override
    public @NotNull Iterator<Optional<Chunk>> iterator() {
        return new Itr(this.world, this.from, this.to);
    }

    public static class Itr implements Iterator<Optional<Chunk>> {
        private final ChunkPos start;
        private final ChunkPos end;
        @NotNull
        private final World world;
        private int currentX;
        private int currentZ;

        private Itr(World world, BlockPos from, BlockPos to) {
            this.world = world;
            WorldChunk minChunk = world.getWorldChunk(from);
            WorldChunk maxChunk = world.getWorldChunk(to);
            ChunkPos minChunkPos = minChunk.getPos();
            ChunkPos maxChunkPos = maxChunk.getPos();
            this.start = minChunkPos;
            this.end = maxChunkPos;
            this.currentX = minChunkPos.x;
            this.currentZ = minChunkPos.z;
        }

        @Override
        public boolean hasNext() {
            return this.currentZ <= end.z;
        }

        @Nullable
        @Override
        public Optional<Chunk> next() {
            if (this.hasNext()) {
                Chunk chunk = this.world.getChunk(currentX, currentZ, ChunkStatus.FULL, false);
                this.currentX++;
                if (currentX > end.x) {
                    this.currentX = start.x;
                    this.currentZ++;
                }
                return Optional.ofNullable(chunk);
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
