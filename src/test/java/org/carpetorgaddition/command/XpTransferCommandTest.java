package org.carpetorgaddition.command;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

public class XpTransferCommandTest {
    // 包私有级成员变量
    static final int MAX_TRANSFER_LEVEL = 21863;
    private static final BigInteger MAX_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);

    @Test
    @Disabled
    public void testTransferAll() {
        MockPlayer from = new MockPlayer(100_0000);
        MockPlayer to = new MockPlayer();
        for (int i = 1; i <= 1000; i++) {
            System.out.println("第" + i + "次");
            transfer(from, to);
            from.clearExperience();
            System.out.println(to);
            MockPlayer temp = from;
            from = to;
            to = temp;
        }
    }

    @Test
    public void testTransferHalf() {
        MockPlayer from = new MockPlayer(3333333);
        MockPlayer to = new MockPlayer();
        this.transferHalf(from, to);
        System.out.println(from);
        System.out.println(to);
    }

    private void transfer(MockPlayer from, MockPlayer to) {
        if (from.getExperienceLevel() <= MAX_TRANSFER_LEVEL) {
            int experience = from.getTotalExperience();
            to.addExperience(experience);
        } else {
            BigInteger experience = from.getTotalExperienceAsBigInteger();
            this.addExperience(to, experience);
        }
    }

    private void transferHalf(MockPlayer from, MockPlayer to) {
        if (from.getExperienceLevel() <= MAX_TRANSFER_LEVEL) {
            int experience = from.getTotalExperience();
            from.clearExperience();
            int half = experience / 2;
            to.addExperience(half);
            from.addExperience(experience - half);
        } else {
            BigInteger experience = from.getTotalExperienceAsBigInteger();
            from.clearExperience();
            BigInteger half = experience.divide(BigInteger.valueOf(2));
            this.addExperience(to, half);
            this.addExperience(from, experience.subtract(half));
        }
    }

    private void addExperience(MockPlayer to, BigInteger experience) {
        while (experience.compareTo(MAX_VALUE) >= 0) {
            experience = experience.subtract(MAX_VALUE);
            to.addExperience(Integer.MAX_VALUE);
        }
        to.addExperience(experience.intValue());
    }
}
