package org.carpetorgaddition.periodic.fakeplayer;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.Action;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import org.carpetorgaddition.mixin.accessor.carpet.EntityPlayerActionPackAccessor;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.TextJoiner;

import java.util.EnumMap;
import java.util.Map;

public class EntityPlayerActionPackSerial {
    private final Map<ActionType, Action> actionMap;
    public static final EntityPlayerActionPackSerial NO_ACTION = new EntityPlayerActionPackSerial();

    private EntityPlayerActionPackSerial() {
        this.actionMap = new EnumMap<>(ActionType.class);
    }

    public EntityPlayerActionPackSerial(EntityPlayerActionPack actionPack) {
        this.actionMap = ((EntityPlayerActionPackAccessor) actionPack).getActions();
    }

    /**
     * 从json中反序列化一个对象
     */
    public EntityPlayerActionPackSerial(JsonObject json) {
        this.actionMap = new EnumMap<>(ActionType.class);
        // 设置假玩家左键
        if (json.has("attack")) {
            JsonObject attack = json.get("attack").getAsJsonObject();
            if (attack.get("continuous").getAsBoolean()) {
                // 左键长按
                this.actionMap.put(ActionType.ATTACK, Action.continuous());
            } else {
                // 间隔左键
                int interval = attack.get("interval").getAsInt();
                this.actionMap.put(ActionType.ATTACK, Action.interval(interval));
            }
        }
        // 设置假玩家右键
        if (json.has("use")) {
            JsonObject attack = json.get("use").getAsJsonObject();
            if (attack.get("continuous").getAsBoolean()) {
                // 右键长按
                this.actionMap.put(ActionType.USE, Action.continuous());
            } else {
                // 间隔右键
                int interval = attack.get("interval").getAsInt();
                this.actionMap.put(ActionType.USE, Action.interval(interval));
            }
        }
    }

    /**
     * 设置假玩家动作
     */
    public void startAction(EntityPlayerMPFake fakePlayer) {
        if (this == NO_ACTION) {
            return;
        }
        EntityPlayerActionPack action = ((ServerPlayerInterface) fakePlayer).getActionPack();
        for (Map.Entry<ActionType, Action> entry : this.actionMap.entrySet()) {
            action.start(entry.getKey(), entry.getValue());
        }
    }

    /**
     * （玩家）是否有动作
     */
    public boolean hasAction() {
        return this == NO_ACTION || !this.actionMap.isEmpty();
    }

    /**
     * 将动作转换为文本
     */
    public Component toText() {
        TextJoiner joiner = new TextJoiner();
        // 左键行为
        Action attack = this.actionMap.get(ActionType.ATTACK);
        if (attack != null) {
            joiner.append("carpet.commands.playerManager.info.left_click");
            joiner.enter(() -> {
                if (((EntityPlayerActionPackAccessor.ActionAccessor) attack).isContinuous()) {
                    // 左键长按
                    joiner.append("carpet.commands.playerManager.info.continuous");
                } else {
                    // 左键单击
                    joiner.append("carpet.commands.playerManager.info.interval", attack.interval);
                }
            });
        }
        // 右键行为
        Action use = this.actionMap.get(ActionType.USE);
        if (use != null) {
            joiner.append(TextBuilder.translate("carpet.commands.playerManager.info.right_click"));
            joiner.enter(() -> {
                if (((EntityPlayerActionPackAccessor.ActionAccessor) use).isContinuous()) {
                    // 右键长按
                    joiner.append("carpet.commands.playerManager.info.continuous");
                } else {
                    // 右键单击
                    joiner.append("carpet.commands.playerManager.info.interval", use.interval);
                }
            });
        }
        return joiner.join();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        // 左键动作
        Action attack = this.actionMap.get(ActionType.ATTACK);
        if (attack != null && !attack.done) {
            JsonObject attackJson = new JsonObject();
            attackJson.addProperty("interval", attack.interval);
            attackJson.addProperty("continuous", ((EntityPlayerActionPackAccessor.ActionAccessor) attack).isContinuous());
            json.add("attack", attackJson);
        }
        // 右键动作
        Action use = this.actionMap.get(ActionType.USE);
        if (use != null && !use.done) {
            JsonObject useJson = new JsonObject();
            useJson.addProperty("interval", use.interval);
            useJson.addProperty("continuous", ((EntityPlayerActionPackAccessor.ActionAccessor) use).isContinuous());
            json.add("use", useJson);
        }
        return json;
    }
}
