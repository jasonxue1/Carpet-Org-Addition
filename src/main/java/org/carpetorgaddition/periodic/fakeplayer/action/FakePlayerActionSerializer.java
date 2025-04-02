package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.GenericFetcherUtils;
import org.carpetorgaddition.util.wheel.TextBuilder;
import org.jetbrains.annotations.NotNull;

public class FakePlayerActionSerializer {
    private final AbstractPlayerAction action;
    public static final FakePlayerActionSerializer NO_ACTION = new FakePlayerActionSerializer();

    private FakePlayerActionSerializer() {
        this.action = StopAction.INSTANCE;
    }

    public FakePlayerActionSerializer(EntityPlayerMPFake fakePlayer) {
        FakePlayerActionManager actionManager = GenericFetcherUtils.getFakePlayerActionManager(fakePlayer);
        this.action = actionManager.getAction();
    }

    public FakePlayerActionSerializer(JsonObject json) {
        for (ActionSerializeType value : ActionSerializeType.values()) {
            if (json.has(value.getSerializedName())) {
                this.action = value.deserialize(json);
                return;
            }
        }
        CarpetOrgAddition.LOGGER.warn("无效的玩家动作数据");
        this.action = StopAction.INSTANCE;
    }

    /**
     * 让假玩家开始执行动作
     */
    public void startAction(@NotNull EntityPlayerMPFake fakePlayer) {
        if (this == NO_ACTION) {
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
        return this.action.isStop();
    }

    public Text toText() {
        TextBuilder builder = new TextBuilder();
        builder.appendTranslate("carpet.commands.playerManager.info.action")
                .newLine()
                .indentation()
                .append(this.action.getDisplayName());
        return builder.toLine();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.action.isHidden()) {
            StopAction stopAction = StopAction.INSTANCE;
            json.add(stopAction.getActionSerializeType().getSerializedName(), stopAction.toJson());
        } else {
            json.add(this.action.getActionSerializeType().getSerializedName(), this.action.toJson());
        }
        return json;
    }
}
