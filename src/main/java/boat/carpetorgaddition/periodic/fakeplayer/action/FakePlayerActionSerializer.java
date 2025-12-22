package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.FetcherUtils;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class FakePlayerActionSerializer {
    private final AbstractPlayerAction action;
    public static final FakePlayerActionSerializer NO_ACTION = new FakePlayerActionSerializer();

    private FakePlayerActionSerializer() {
        this.action = new StopAction(null);
    }

    public FakePlayerActionSerializer(EntityPlayerMPFake fakePlayer) {
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
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
        if (this.action.equalFakePlayer(null)) {
            this.action.setFakePlayer(fakePlayer);
        } else if (!this.action.equalFakePlayer(fakePlayer)) {
            throw new IllegalArgumentException();
        }
        FakePlayerActionManager actionManager = FetcherUtils.getFakePlayerActionManager(fakePlayer);
        actionManager.setAction(this.action);
    }

    public void clearPlayer() {
        this.action.clearFakePlayer();
    }

    public boolean hasAction() {
        return !this.action.isStop();
    }

    public Component getDisplayName() {
        return this.action.getDisplayName();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.action.isHidden() && !CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            StopAction stopAction = new StopAction(null);
            json.add(stopAction.getActionSerializeType().getSerializedName(), stopAction.toJson());
        } else {
            json.add(this.action.getActionSerializeType().getSerializedName(), this.action.toJson());
        }
        return json;
    }
}
