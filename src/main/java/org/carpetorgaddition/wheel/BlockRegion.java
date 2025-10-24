package org.carpetorgaddition.wheel;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.carpetorgaddition.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 用来获取指定范围内所有方块坐标对象，方块坐标对象不是使用集合一次性返回的，
 * 而是使用迭代器逐个返回，因此它不会大量占用内存，并且本类实现了{@link Iterable}接口，可以使用增强for循环遍历
 */
public class BlockRegion implements Iterable<BlockPos> {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public BlockRegion(World world, BlockPos sourcePos, int range) {
        this.minX = sourcePos.getX() - Math.abs(range);
        this.minY = world.getBottomY();
        this.minZ = sourcePos.getZ() - Math.abs(range);
        this.maxX = sourcePos.getX() + Math.abs(range);
        this.maxY = world.getTopYInclusive();
        this.maxZ = sourcePos.getZ() + Math.abs(range);
    }

    public BlockRegion(BlockPos from, BlockPos to) {
        this.minX = Math.min(from.getX(), to.getX());
        this.minY = Math.min(from.getY(), to.getY());
        this.minZ = Math.min(from.getZ(), to.getZ());
        this.maxX = Math.max(from.getX(), to.getX());
        this.maxY = Math.max(from.getY(), to.getY());
        this.maxZ = Math.max(from.getZ(), to.getZ());
    }

    public BlockRegion(Box box) {
        this.minX = (int) box.minX;
        this.minY = (int) box.minY;
        this.minZ = (int) box.minZ;
        this.maxX = (int) box.maxX;
        this.maxY = (int) box.maxY;
        this.maxZ = (int) box.maxZ;
    }

    /**
     * @return 选区内方块的总数
     */
    public int size() {
        return (this.maxX - this.minX + 1) * (this.maxY - this.minY + 1) * (this.maxZ - this.minZ + 1);
    }

    /**
     * @return 与当前对象等效的Box对象
     */
    public Box toBox() {
        return new Box(this.minX, this.minY, this.minZ, this.maxX + 1, this.maxY + 1, this.maxZ + 1);
    }

    /**
     * @return 该选区是否包含指定位置
     */
    public boolean contains(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    /**
     * @return 随机获取一个方块坐标
     */
    public BlockPos randomBlockPos() {
        int x = MathUtils.randomInt(this.minX, this.maxX);
        int y = MathUtils.randomInt(this.minY, this.maxY);
        int z = MathUtils.randomInt(this.minZ, this.maxZ);
        return new BlockPos(x, y, z);
    }

    /**
     * @return XYZ轴正方向最大的方块坐标
     */
    public BlockPos getMaxBlockPos() {
        return new BlockPos(this.maxX, this.maxY, this.maxZ);
    }

    /**
     * @return XYZ轴正方向最小的方块坐标
     */
    public BlockPos getMinBlockPos() {
        return new BlockPos(this.minX, this.minY, this.minZ);
    }

    /**
     * 类对象是不可变的，因此不需要考虑并发修改的问题
     */
    @NotNull
    @Override
    public Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            /**
             * 当前迭代次数
             */
            private int iterations = 0;
            /**
             * 最大迭代次数
             */
            private final int maxIterations = BlockRegion.this.size();
            private final int startX = minX;
            private final int startY = minY;
            private final int startZ = minZ;
            private final int finalX = maxX;
            private final int finalY = maxY;
            private final int finalZ = maxZ;
            // 迭代器当前遍历到的位置
            private int currentX = startX;
            private int currentY = startY;
            private int currentZ = startZ;

            @Override
            public boolean hasNext() {
                // 当前方块坐标是否在选区内
                return this.iterations < maxIterations;
            }

            @Override
            public BlockPos next() {
                if (!hasNext()) {
                    // 超出选区抛出异常
                    throw new NoSuchElementException();
                }
                this.iterations++;
                this.currentX++;
                // X轴遍历到了最后，X重置，Y递增，Z轴不变
                if (this.currentX > this.finalX) {
                    this.currentX = this.startX;
                    this.currentY++;
                    if (this.currentY > this.finalY) {
                        this.currentY = this.startY;
                        this.currentZ++;
                        if (this.currentZ > this.finalZ) {
                            // Z轴也遍历到了最后，直接将最大坐标返回
                            return getMaxBlockPos();
                        }
                    }
                }
                return new BlockPos(this.currentX, this.currentY, this.currentZ);
            }
        };
    }
}
