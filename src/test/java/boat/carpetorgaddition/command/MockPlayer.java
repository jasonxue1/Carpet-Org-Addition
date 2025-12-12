package boat.carpetorgaddition.command;

import net.minecraft.util.Mth;

import java.math.BigDecimal;
import java.math.BigInteger;

public class MockPlayer {
    private int experienceLevel;
    private int totalExperience;
    private float experienceProgress;

    public MockPlayer(int level) {
        this.addExperienceLevels(level);
    }

    public void addExperienceLevels(int levels) {
        this.experienceLevel += levels;
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
            this.experienceProgress = 0.0F;
            this.totalExperience = 0;
        }
    }

    public int getNextLevelExperience() {
        if (this.experienceLevel >= 30) {
            return 112 + (this.experienceLevel - 30) * 9;
        } else {
            return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
        }
    }

    public void addExperience(int experience) {
        this.experienceProgress = this.experienceProgress + (float) experience / (float) this.getNextLevelExperience();
        this.totalExperience = Mth.clamp(this.totalExperience + experience, 0, Integer.MAX_VALUE);
        while (this.experienceProgress < 0.0F) {
            float f = this.experienceProgress * (float) this.getNextLevelExperience();
            if (this.experienceLevel > 0) {
                this.addExperienceLevels(-1);
                this.experienceProgress = 1.0F + f / (float) this.getNextLevelExperience();
            } else {
                this.addExperienceLevels(-1);
                this.experienceProgress = 0.0F;
            }
        }
        while (this.experienceProgress >= 1.0F) {
            this.experienceProgress = (this.experienceProgress - 1.0F) * (float) this.getNextLevelExperience();
            this.addExperienceLevels(1);
            this.experienceProgress = this.experienceProgress / (float) this.getNextLevelExperience();
        }
    }

    public int getPoint() {
        return Mth.floor(this.experienceProgress * (float) this.getNextLevelExperience());
    }

    public void clearExperience() {
        this.experienceLevel = 0;
        this.experienceProgress = 0;
    }

    public int getExperienceLevel() {
        return experienceLevel;
    }

    public BigInteger getTotalExperienceAsBigInteger() {
        int level = this.experienceLevel;
        int xp = this.getPoint();
        if (level <= XpTransferCommandTest.MAX_TRANSFER_LEVEL) {
            throw new IllegalStateException();
        }
        BigDecimal bigLevel = BigDecimal.valueOf(level);
        BigDecimal decimal = new BigDecimal("4.5").multiply(bigLevel.multiply(bigLevel))
                .subtract(new BigDecimal("162.5").multiply(bigLevel))
                .add(new BigDecimal("2220"))
                .add(new BigDecimal(xp));
        return decimal.multiply(new BigDecimal("0.99999999328")).toBigInteger();
    }

    @Override
    public String toString() {
        return "MockPlayer{等级=" + experienceLevel + ", 进度=" + experienceProgress + ", 经验值="
               + (this.experienceLevel <= XpTransferCommandTest.MAX_TRANSFER_LEVEL ? this.getExperienceLevel() : this.getTotalExperienceAsBigInteger())
               + '}';
    }
}
