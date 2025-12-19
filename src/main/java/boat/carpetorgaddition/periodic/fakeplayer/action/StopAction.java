package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class StopAction extends AbstractPlayerAction {
    public StopAction(EntityPlayerMPFake fakePlayer) {
        super(fakePlayer);
    }

    @Override
    protected void tick() {
        // 什么也不做
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
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
    public Component getDisplayName() {
        return TextBuilder.translate("carpet.commands.playerAction.action.stop");
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.STOP;
    }
}
