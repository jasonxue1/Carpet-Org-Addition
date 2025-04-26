package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.TextUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public final class StopAction extends AbstractPlayerAction {
    @Deprecated(forRemoval = true)
    public static final StopAction INSTANCE = new StopAction(null);

    public StopAction(@Nullable EntityPlayerMPFake fakePlayer) {
        super(fakePlayer);
    }

    @Override
    public void tick() {
        // 什么也不做
    }

    @Override
    public ArrayList<MutableText> info() {
        ArrayList<MutableText> list = new ArrayList<>();
        // 直接将假玩家没有任何动作的信息加入集合然后返回
        list.add(TextUtils.translate("carpet.commands.playerAction.info.stop", this.fakePlayer.getDisplayName()));
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
        return TextUtils.translate("carpet.commands.playerAction.action.stop");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.STOP;
    }
}
