package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.ArrayList;

public final class StopAction extends AbstractPlayerAction {
    public StopAction(EntityPlayerMPFake fakePlayer) {
        super(fakePlayer);
    }

    @Override
    protected void tick() {
        // 什么也不做
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        // 直接将假玩家没有任何动作的信息加入集合然后返回
        list.add(TextBuilder.translate("carpet.commands.playerAction.info.stop", this.getFakePlayer().getDisplayName()));
        return list;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }

    @Override
    public boolean isStop() {
        return true;
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.stop");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.STOP;
    }
}
