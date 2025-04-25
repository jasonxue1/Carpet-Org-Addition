package org.carpetorgaddition.util.wheel;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.carpetorgaddition.command.XpTransferCommand;
import org.carpetorgaddition.exception.OperationTimeoutException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Supplier;

public record ExperienceTransfer(ServerPlayerEntity player) {
    /**
     * {@code int}的最大值
     */
    private static final BigInteger MAX_INTEGER_VALUE = BigInteger.valueOf(Integer.MAX_VALUE);
    /**
     * 通过添加经验值能合法提升的最高经验等级
     */
    private static final int MAX_EFFECTIVE_LEVEL = 238609312;
    /**
     * 要转移的等级数超过该值后修正经验值
     */
    private static final BigDecimal THRESHOLD = new BigDecimal("100000");
    /**
     * 经验转移的最大时间
     */
    private static final long TIMEOUT_MILLIS = 5000L;

    /**
     * @return 获取玩家的经验值数
     */
    private int getPoint() {
        int result = MathHelper.floor(this.player.experienceProgress * (float) this.player.getNextLevelExperience());
        // 避免数值溢出
        return Math.max(result, 0);
    }

    /**
     * 清除玩家的所有经验
     */
    private void clearExperience() {
        this.player.setExperienceLevel(0);
        this.player.setExperiencePoints(0);
    }

    /**
     * 将一名玩家的经验全部转移给另一名玩家
     *
     * @return 转移经验的数量
     */
    public Number transferAllTo(ExperienceTransfer transfer) {
        if (this.isLowLevel()) {
            // 转移少量经验
            int experience = this.extractAllExperience();
            transfer.addExperience(experience);
            return experience;
        } else {
            // 转移大量经验
            return this.transfer(() -> {
                final BigInteger result = this.extractAllExperienceAsBigInteger();
                this.addExperience(transfer, result);
                return result;
            });
        }
    }

    /**
     * 将一名玩家一半的经验转移给另一名玩家
     *
     * @return 转移经验的数量
     */
    public Number transferHalfTo(ExperienceTransfer transfer) {
        if (this.isLowLevel()) {
            int experience = this.extractAllExperience();
            int half = experience / 2;
            // 转移一半经验，然后将另一半重新添加到当前玩家
            transfer.addExperience(half);
            this.addExperience(experience - half);
            return half;
        } else {
            return this.transfer(() -> {
                BigInteger experience = this.extractAllExperienceAsBigInteger();
                BigInteger half = experience.divide(BigInteger.TWO);
                long time = System.currentTimeMillis();
                this.addExperience(transfer, half, TIMEOUT_MILLIS);
                long timeElapsed = System.currentTimeMillis() - time;
                this.addExperience(this, experience.subtract(half), TIMEOUT_MILLIS - timeElapsed);
                return half;
            });
        }
    }

    /**
     * 转移指定数量的经验
     */
    public void transferTo(ExperienceTransfer transfer, final int count) {
        if (this.isLowLevel()) {
            int total = this.calculateTotalExperience();
            if (count > total) {
                throw new ExperienceTransferException(count, total);
            }
            this.clearExperience();
            this.addExperience(total - count);
        } else {
            this.transfer(() -> {
                BigInteger total = this.calculateTotalExperienceAsBigInteger();
                BigInteger value = BigInteger.valueOf(count);
                if (value.compareTo(total) > 0) {
                    throw new ExperienceTransferException(count, total);
                }
                this.clearExperience();
                this.addExperience(total.subtract(value));
                return count;
            });
        }
        transfer.addExperience(count);
    }

    private Number transfer(Supplier<Number> supplier) {
        int level = this.player.experienceLevel;
        int point = this.getPoint();
        try {
            return supplier.get();
        } catch (OperationTimeoutException e) {
            this.player.setExperienceLevel(level);
            this.player.setExperiencePoints(point);
            throw e;
        }
    }

    /**
     * @return 当前玩家是否是假玩家或指定玩家
     */
    public boolean isSpecifiedOrFakePlayer(ServerPlayerEntity specified) {
        return this.player instanceof EntityPlayerMPFake || this.player == specified;
    }

    private boolean isLowLevel() {
        return this.player.experienceLevel <= XpTransferCommand.MAX_TRANSFER_LEVEL;
    }

    /**
     * 添加经验
     *
     * @param experience 添加经验的数量
     */
    private void addExperience(int experience) {
        this.player.addExperience(experience);
    }

    private void addExperience(BigInteger bigInteger) {
        this.addExperience(this, bigInteger);
    }

    private void addExperience(ExperienceTransfer transfer, BigInteger experience) {
        this.addExperience(transfer, experience, TIMEOUT_MILLIS);
    }

    private void addExperience(ExperienceTransfer transfer, BigInteger experience, long timeout) {
        int level = transfer.player.experienceLevel;
        int point = transfer.getPoint();
        try {
            long time = System.currentTimeMillis();
            while (experience.compareTo(MAX_INTEGER_VALUE) >= 0) {
                // 经验转移必须在指定时间内完成
                if (System.currentTimeMillis() - time >= timeout) {
                    throw new OperationTimeoutException();
                }
                experience = experience.subtract(MAX_INTEGER_VALUE);
                transfer.addExperience(Integer.MAX_VALUE);
            }
            transfer.addExperience(experience.intValue());
        } catch (OperationTimeoutException e) {
            // 操作超时，回退经验
            transfer.player.setExperienceLevel(level);
            transfer.player.setExperiencePoints(point);
            throw e;
        }
    }

    /**
     * 提取玩家的所有经验值
     */
    private int extractAllExperience() {
        int experience = calculateTotalExperience();
        this.clearExperience();
        return experience;
    }

    /**
     * 计算玩家的经验值
     *
     * @return 总经验值
     * @author ChatGPT
     */
    private int calculateTotalExperience() {
        int level = this.player.experienceLevel;
        int point = this.getPoint();
        return calculateTotalExperience(level, point);
    }

    /**
     * 计算并清空玩家的经验值
     *
     * @return 玩家的总经验数
     */
    private BigInteger extractAllExperienceAsBigInteger() {
        BigInteger experience = calculateTotalExperienceAsBigInteger();
        // 清除玩家的所有经验
        this.clearExperience();
        return experience;
    }

    /**
     * 计算玩家的总经验值
     *
     * @apiNote 转移大量经验时，实际转移的经验数量可能高于预期，因此方法返回结果前可能会对超出部分的经验进行舍弃
     * @see <a href="https://report.bugs.mojang.com/servicedesk/customer/portal/2/MC-271084">MC-271084</a>
     */
    private BigInteger calculateTotalExperienceAsBigInteger() {
        int level = Math.min(this.player.experienceLevel, MAX_EFFECTIVE_LEVEL);
        int xp = this.player.experienceLevel >= MAX_EFFECTIVE_LEVEL ? 0 : this.getPoint();
        if (level <= XpTransferCommand.MAX_TRANSFER_LEVEL) {
            throw new IllegalStateException();
        }
        BigDecimal bigLevel = BigDecimal.valueOf(level);
        BigDecimal decimal = new BigDecimal("4.5").multiply(bigLevel.multiply(bigLevel))
                .subtract(new BigDecimal("162.5").multiply(bigLevel))
                .add(new BigDecimal("2220"))
                .add(new BigDecimal(xp));
        BigDecimal factor = new BigDecimal("0.99999999328");
        return decimal.compareTo(THRESHOLD) <= 0 ? decimal.toBigInteger() : decimal.multiply(factor).toBigInteger();
    }

    public int getLevel() {
        return this.player.experienceLevel;
    }

    /**
     * 根据经验等级和经验值计算总经验值<br>
     *
     * @param level 经验等级
     * @param xp    经验值
     * @return 总经验值
     * @author ChatGPT
     */
    public static int calculateTotalExperience(int level, int xp) {
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
        return totalExp + xp;
    }

    public static class ExperienceTransferException extends RuntimeException {
        private final Number require;
        private final Number existing;

        public ExperienceTransferException(Number require, Number existing) {
            this.require = require;
            this.existing = existing;
        }

        public Number getRequire() {
            return require;
        }

        public Number getExisting() {
            return existing;
        }
    }
}
