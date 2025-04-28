package org.carpetorgaddition.periodic.fakeplayer.action.bedrock;

import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class BedrockBreakingContext {
    private final BlockPos bedrockPos;
    private BlockPos leverPos;
    private BreakingState breakingState = BreakingState.PLACE_THE_PISTON_FACING_UP;
    private boolean isInstantlyDestroyed = false;

    public BedrockBreakingContext(BlockPos bedrockPos) {
        this.bedrockPos = bedrockPos;
    }

    public BlockPos getBedrockPos() {
        return this.bedrockPos;
    }

    public BlockPos getLeverPos() {
        return this.leverPos;
    }

    public void setLeverPos(BlockPos leverPos) {
        this.leverPos = leverPos;
    }

    public BreakingState getState() {
        return this.breakingState;
    }

    public void nextStep() {
        BreakingState[] values = BreakingState.values();
        if (this.breakingState.ordinal() == values.length) {
            throw new IllegalStateException();
        }
        this.breakingState = values[this.breakingState.ordinal() + 1];
    }

    public void fail() {
        this.breakingState = BreakingState.COMPLETE;
    }

    public boolean isInstantlyDestroyed() {
        return isInstantlyDestroyed;
    }

    public void setInstantlyDestroyed(boolean instantlyDestroyed) {
        isInstantlyDestroyed = instantlyDestroyed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BedrockBreakingContext that = (BedrockBreakingContext) o;
        return Objects.equals(bedrockPos, that.bedrockPos);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bedrockPos);
    }
}
