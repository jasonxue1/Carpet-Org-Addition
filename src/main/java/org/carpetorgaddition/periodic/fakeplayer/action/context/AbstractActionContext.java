package org.carpetorgaddition.periodic.fakeplayer.action.context;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;

import java.util.ArrayList;

public abstract class AbstractActionContext {
    public abstract ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer);

    /**
     * 序列化假玩家动作数据
     */
    public abstract JsonObject toJson();
}
