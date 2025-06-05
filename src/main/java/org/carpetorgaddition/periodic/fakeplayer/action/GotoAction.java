package org.carpetorgaddition.periodic.fakeplayer.action;

import com.google.gson.JsonObject;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerPathfinder;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.ArrayList;

public class GotoAction extends AbstractPlayerAction {
    private final FakePlayerPathfinder pathfinder;

    public GotoAction(FakePlayerPathfinder pathfinder) {
        super(pathfinder.getFakePlayer());
        this.pathfinder = pathfinder;
    }

    @Override
    public void tick() {
        this.pathfinder.tick();
    }

    @Override
    public ArrayList<MutableText> info() {
        // TODO 玩家动作信息
        return new ArrayList<>();
    }

    @Override
    public JsonObject toJson() {
        // 不保存玩家数据
        return new JsonObject();
    }

    @Override
    public MutableText getDisplayName() {
        return TextBuilder.empty();
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.GOTO;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public void onStop() {
        this.pathfinder.onStop();
    }
}
