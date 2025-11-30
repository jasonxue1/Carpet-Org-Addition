package org.carpetorgaddition.wheel.traverser;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public class BlockEntityTraverser extends WorldTraverser<BlockEntity> {
    private final World world;

    public BlockEntityTraverser(World world, BlockPos sourcePos, int range) {
        super(world, sourcePos, range);
        this.world = world;
    }

    public BlockEntityTraverser(World world, BlockPos from, BlockPos to) {
        super(from, to);
        this.world = world;
    }

    @Override
    public @NotNull Iterator<BlockEntity> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<BlockEntity> {
        private BlockEntity next;
        private Iterator<BlockEntity> current;
        private boolean alreadyNext = false;
        private final Iterator<Optional<Chunk>> chunkIterator;

        private Itr() {
            ChunkTraverser traverser = new ChunkTraverser(world, from, to);
            this.chunkIterator = traverser.iterator();
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
                    Optional<WorldChunk> optional = this.chunkIterator.next()
                            .filter(chunk -> chunk instanceof WorldChunk)
                            .map(chunk -> (WorldChunk) chunk);
                    // 区块可能未加载
                    if (optional.isEmpty()) {
                        continue;
                    }
                    this.current = optional.get().getBlockEntities().values().iterator();
                }
                while (this.current.hasNext()) {
                    BlockEntity blockEntity = this.current.next();
                    if (BlockEntityTraverser.this.contains(blockEntity.getPos())) {
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
