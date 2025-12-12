package boat.carpetorgaddition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CarpetOrgAdditionTest {
    /**
     * 检查构建时Java的版本，构建时Java版本应等于Minecraft支持的最低Java版本
     */
    @Test
    public void testJavaVersion() {
        Assertions.assertEquals(21, Runtime.version().version().getFirst().intValue(), "请使用jdk21构建");
    }
}
