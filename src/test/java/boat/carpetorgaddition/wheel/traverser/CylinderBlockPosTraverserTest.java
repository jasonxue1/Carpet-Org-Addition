package boat.carpetorgaddition.wheel.traverser;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CylinderBlockPosTraverserTest {
    @Test
    public void testRandomBlockPos() {
        CylinderBlockPosTraverser region = new CylinderBlockPosTraverser(new BlockPos(0, 0, 0), 1, 1);
        System.out.println(region.randomBlockPos());
    }

    @Test
    public void testIterator() {
        BlockPos center = new BlockPos(0, 0, 0);
        Assertions.assertEquals(0, new CylinderBlockPosTraverser(center, 15, 0).size());
        Assertions.assertEquals(9, new CylinderBlockPosTraverser(center, 1, 1).size());
        Assertions.assertEquals(21, new CylinderBlockPosTraverser(center, 2, 1).size());
        Assertions.assertEquals(37, new CylinderBlockPosTraverser(center, 3, 1).size());
        Assertions.assertEquals(74, new CylinderBlockPosTraverser(center, 3, 2).size());
        Assertions.assertEquals(111, new CylinderBlockPosTraverser(center, 3, 3).size());
    }
}
