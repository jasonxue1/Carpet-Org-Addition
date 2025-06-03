package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerGotoPath;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.ArrayList;

public class GotoAction extends AbstractPlayerAction {
    private final FakePlayerGotoPath path;
    /**
     * 距离上一次滞空后经历的时间
     */
    private int notGroundTime = 0;

    public GotoAction(EntityPlayerMPFake fakePlayer, FakePlayerGotoPath path) {
        super(fakePlayer);
        this.path = path;
    }

    @Override
    public void tick() {
        if (this.path.isFinished()) {
            return;
        }
        Vec3d current = this.path.getExpectedPos();
        if (this.fakePlayer.isOnGround()) {
            if (this.notGroundTime <= 0) {
                this.fakePlayer.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, current);
            }
            this.notGroundTime--;
        } else {
            // 玩家在从一格高的方块上下来时会尝试回到上一个节点
            this.notGroundTime = 2;
        }
        Vec3d pos = this.fakePlayer.getPos();
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
        // 玩家到达了当前节点
        if (this.path.arrivedAtAnyNode()) {
            actionPack.setForward(0F);
            return;
        }
        actionPack.setForward(1F);
        this.jump(current, pos);
    }

    private void jump(Vec3d current, Vec3d pos) {
        double horizontal = MathUtils.horizontalDistance(current, pos);
        double vertical = MathUtils.verticalDistance(current, pos);
        // 玩家可以直接走向方块，不需要跳跃
        if (vertical <= fakePlayer.getAttributeValue(EntityAttributes.GENERIC_STEP_HEIGHT)) {
            return;
        }
        // 当前位置比玩家位置低，不需要跳跃
        if (current.getY() < pos.getY()) {
            return;
        }
        // 跳跃高度可能受多种因素影响，但这里不考虑它
        if (horizontal < 1.0 && vertical < 1.25) {
            this.fakePlayer.jump();
        }
    }

    @Override
    public ArrayList<MutableText> info() {
        return new ArrayList<>();
    }

    @Override
    public JsonObject toJson() {
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
}
