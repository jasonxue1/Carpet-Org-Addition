package org.carpetorgaddition.periodic.fakeplayer.action.temp;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;

import java.util.ArrayList;

public abstract class AbstractPlayerAction {
    protected final EntityPlayerMPFake fakePlayer;

    public AbstractPlayerAction(EntityPlayerMPFake fakePlayer) {
        this.fakePlayer = fakePlayer;
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

    /**
     * 获取序列化名称
     */
    public abstract String getSerializedName();

    /**
     * 当前动作是否是隐藏的
     */
    public boolean isHidden() {
        return false;
    }
}
