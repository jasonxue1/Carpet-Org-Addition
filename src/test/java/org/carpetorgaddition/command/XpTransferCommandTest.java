package org.carpetorgaddition.command;

import org.carpetorgaddition.util.wheel.ExperienceTransfer;
import org.junit.jupiter.api.Assertions;
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
    @Disabled
    public void testTransferHalf() {
        MockPlayer from = new MockPlayer(3333333);
        MockPlayer to = new MockPlayer();
        this.transferHalf(from, to);
        System.out.println(from);
        System.out.println(to);
    }

    @Test
    public void testCalculateUpgradeExperience() {
        Assertions.assertEquals(BigInteger.valueOf(7), ExperienceTransfer.calculateUpgradeExperience(0, 1));
        Assertions.assertEquals(BigInteger.valueOf(64), ExperienceTransfer.calculateUpgradeExperience(3, 7));
        Assertions.assertEquals(BigInteger.valueOf(682), ExperienceTransfer.calculateUpgradeExperience(15, 26));
        Assertions.assertEquals(BigInteger.valueOf(9702), ExperienceTransfer.calculateUpgradeExperience(20, 64));
        Assertions.assertEquals(BigInteger.valueOf(30963), ExperienceTransfer.calculateUpgradeExperience(1, 100));
        Assertions.assertEquals(BigInteger.valueOf(358470), ExperienceTransfer.calculateUpgradeExperience(0, 300));
        Assertions.assertEquals(BigInteger.valueOf(1740870), ExperienceTransfer.calculateUpgradeExperience(20, 640));
        Assertions.assertEquals(BigInteger.valueOf(4308750), ExperienceTransfer.calculateUpgradeExperience(100, 1000));
        Assertions.assertEquals(BigInteger.valueOf(444037500), ExperienceTransfer.calculateUpgradeExperience(1000, 10000));
        Assertions.assertEquals(BigInteger.valueOf(2147407943), ExperienceTransfer.calculateUpgradeExperience(0, 21863));
        Assertions.assertEquals(BigInteger.valueOf(4499837502220L), ExperienceTransfer.calculateUpgradeExperience(0, 1000000));
        Assertions.assertEquals(BigInteger.valueOf(256204778204999070L), ExperienceTransfer.calculateUpgradeExperience(0, 238609312));
        Assertions.assertEquals(BigInteger.valueOf(-30963), ExperienceTransfer.calculateUpgradeExperience(100, 1));
        Assertions.assertEquals(BigInteger.valueOf(-4339720), ExperienceTransfer.calculateUpgradeExperience(1000, 0));
        Assertions.assertThrows(ArithmeticException.class, () -> ExperienceTransfer.calculateUpgradeExperience(0, 238609313));
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
