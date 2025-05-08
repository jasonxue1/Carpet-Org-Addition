package org.carpetorgaddition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RuleSelfConstantsTest {
    /**
     * 测试常量类中有没有无法反射获取的字段
     */
    @Test
    public void testInit() {
        // 加载类
        Assertions.assertDoesNotThrow(() -> Class.forName("org.carpetorgaddition.rule.RuleSelfConstants"));
    }
}
