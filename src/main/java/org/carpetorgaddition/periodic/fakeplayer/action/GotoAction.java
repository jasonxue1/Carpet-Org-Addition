package org.carpetorgaddition.periodic.fakeplayer.action;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerGotoPath;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.ArrayList;

public class GotoAction extends AbstractPlayerAction {
    private final FakePlayerGotoPath path;

    public GotoAction(EntityPlayerMPFake fakePlayer, FakePlayerGotoPath path) {
        super(fakePlayer);
        this.path = path;
    }

    @Override
    public void tick() {
        if (this.path.canStart()) {
            if (this.path.isFinished()) {
                return;
            }
            Vec3d expected = this.path.getExpectedPos();
            this.fakePlayer.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, expected);
            Vec3d vec3d = expected.subtract(this.fakePlayer.getPos());
            EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
            if (vec3d.length() <= 0.5) {
                actionPack.setForward(0F);
                this.path.next();
                return;
            }
            actionPack.setForward(1F);
        } else {
            this.path.straightTravel();
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
