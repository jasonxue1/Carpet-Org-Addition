package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class AbstractPlayerAction {
    @Nullable
    private EntityPlayerMPFake fakePlayer;
    private boolean isPlayerChanged = false;

    public AbstractPlayerAction(@Nullable EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
        if (this.fakePlayer != null) {
            this.isPlayerChanged = true;
        }
    }

    public final void execute() {
        if (this.isPlayerChanged) {
            this.onAssignPlayer();
            this.isPlayerChanged = false;
        }
        Objects.requireNonNull(this.fakePlayer);
        if (this.isValid()) {
            this.tick();
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * 每个游戏刻都执行
     */
    protected abstract void tick();

    /**
     * 当前动作的详细信息
     */
    public abstract List<Text> info();

    /**
     * 序列化假玩家动作数据
     */
    public abstract JsonObject toJson();

    /**
     * @return 当前动作类型的显示名称
     */
    public abstract Text getDisplayName();

    public abstract ActionSerializeType getActionSerializeType();

    /**
     * 当前动作是否是隐藏的
     */
    public boolean isHidden() {
        return false;
    }

    public boolean isValid() {
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            return true;
        }
        return !this.isHidden();
    }

    public boolean isStop() {
        return false;
    }

    /**
     * 当玩家停止当前动作时调用
     */
    public void onStop() {
    }

    @NotNull
    protected EntityPlayerMPFake getFakePlayer() {
        return Objects.requireNonNull(this.fakePlayer);
    }

    public boolean equalFakePlayer(@Nullable EntityPlayerMPFake fakePlayer) {
        return Objects.equals(this.fakePlayer, fakePlayer);
    }

    @Contract("null -> fail")
    public void setFakePlayer(EntityPlayerMPFake fakePlayer) {
        if (fakePlayer == null) {
            throw new IllegalArgumentException();
        }
        this.fakePlayer = fakePlayer;
        this.onAssignPlayer();
    }

    public void clearFakePlayer() {
        this.fakePlayer = null;
        this.onClearPlayer();
    }

    /**
     * 当玩家被赋值时调用
     */
    protected void onAssignPlayer() {
    }

    /**
     * 当玩家被赋值为{@code null}时调用
     */
    protected void onClearPlayer() {
    }
}
