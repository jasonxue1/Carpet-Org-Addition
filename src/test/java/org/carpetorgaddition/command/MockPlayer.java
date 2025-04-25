package org.carpetorgaddition.command;

import net.minecraft.util.math.MathHelper;

import java.math.BigDecimal;
import java.math.BigInteger;

public class MockPlayer {
    private int experienceLevel;
    private int totalExperience;
    private float experienceProgress;

    public MockPlayer() {
    }

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
        this.totalExperience = MathHelper.clamp(this.totalExperience + experience, 0, Integer.MAX_VALUE);
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
        return MathHelper.floor(this.experienceProgress * (float) this.getNextLevelExperience());
    }

    public void clearExperience() {
        this.experienceLevel = 0;
        this.experienceProgress = 0;
    }

    public void setExperiencePoints(int points) {
        float f = (float) this.getNextLevelExperience();
        float g = (f - 1.0F) / f;
        this.experienceProgress = MathHelper.clamp((float) points / f, 0.0F, g);
    }

    public int getExperienceLevel() {
        return experienceLevel;
    }

    public float getExperienceProgress() {
        return experienceProgress;
    }

    public int getTotalExperience() {
        int level = this.experienceLevel;
        int xp = this.getPoint();
        int totalExp;
        // 0-16级
        if (level <= 16) {
            totalExp = level * level + 6 * level;
        }
        // 17-31级
        else if (level <= 31) {
            totalExp = (int) (2.5 * level * level - 40.5 * level + 360);
        }
        // 32级以上
        else {
            totalExp = (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        // 防止数值溢出
        int sum = totalExp + xp;
        return sum < 0 ? totalExp : sum;
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
