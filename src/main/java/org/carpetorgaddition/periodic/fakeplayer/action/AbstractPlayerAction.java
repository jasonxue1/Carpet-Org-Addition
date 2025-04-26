package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.CarpetOrgAddition;

import java.util.ArrayList;

public abstract class AbstractPlayerAction {
    protected EntityPlayerMPFake fakePlayer;

    public AbstractPlayerAction(EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
    }

    public final void execute() {
        if (this.isValid()) {
            this.tick();
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * 每个游戏刻都执行
     */
    public abstract void tick();

    /**
     * 当前动作的详细信息
     */
    public abstract ArrayList<MutableText> info();

    /**
     * 序列化假玩家动作数据
     */
    public abstract JsonObject toJson();

    /**
     * @return 当前动作类型的显示名称
     */
    public abstract MutableText getDisplayName();

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

    public EntityPlayerMPFake getFakePlayer() {
        return fakePlayer;
    }

    public void setFakePlayer(EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
    }
}
