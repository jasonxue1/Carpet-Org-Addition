package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.GenericFetcherUtils;
import org.carpetorgaddition.util.provider.TextProvider;
import org.carpetorgaddition.util.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class FakePlayerActionSerializer {
    private final AbstractPlayerAction action;
    public static final FakePlayerActionSerializer NO_ACTION = new FakePlayerActionSerializer();

    private FakePlayerActionSerializer() {
        this.action = new StopAction(null);
    }

    public FakePlayerActionSerializer(EntityPlayerMPFake fakePlayer) {
        FakePlayerActionManager actionManager = GenericFetcherUtils.getFakePlayerActionManager(fakePlayer);
        this.action = actionManager.getAction();
    }

    public FakePlayerActionSerializer(JsonObject json) {
        for (ActionSerializeType value : ActionSerializeType.values()) {
            String serializedName = value.getSerializedName();
            if (json.has(serializedName)) {
                JsonObject actionJson = json.getAsJsonObject(serializedName);
                AbstractPlayerAction deserialize = value.deserialize(actionJson);
                if (deserialize.isValid()) {
                    this.action = deserialize;
                    return;
                }
                break;
            }
        }
        this.action = new StopAction(null);
    }

    /**
     * 让假玩家开始执行动作
     */
    public void startAction(@NotNull EntityPlayerMPFake fakePlayer) {
        if (this == NO_ACTION || this.action.isStop()) {
            return;
        }
        if (this.action.getFakePlayer() == null) {
            this.action.setFakePlayer(fakePlayer);
        } else if (!this.action.getFakePlayer().equals(fakePlayer)) {
            throw new IllegalArgumentException();
        }
        FakePlayerActionManager actionManager = GenericFetcherUtils.getFakePlayerActionManager(fakePlayer);
        actionManager.setAction(this.action);
    }

    public boolean hasAction() {
        return !this.action.isStop();
    }

    public Text toText() {
        ArrayList<Text> list = new ArrayList<>();
        list.add(TextBuilder.translate("carpet.commands.playerManager.info.action"));
        list.add(TextProvider.NEW_LINE);
        list.add(TextBuilder.combineAll(TextProvider.INDENT_SYMBOL, this.action.getDisplayName()));
        return TextBuilder.combineList(list);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.action.isHidden()) {
            StopAction stopAction = new StopAction(null);
            json.add(stopAction.getActionSerializeType().getSerializedName(), stopAction.toJson());
        } else {
            json.add(this.action.getActionSerializeType().getSerializedName(), this.action.toJson());
        }
        return json;
    }
}
