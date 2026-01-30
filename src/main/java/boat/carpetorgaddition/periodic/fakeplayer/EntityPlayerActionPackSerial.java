package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.mixin.accessor.carpet.EntityPlayerActionPackAccessor;
import boat.carpetorgaddition.mixin.accessor.carpet.EntityPlayerActionPackAccessor.ActionAccessor;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.helpers.EntityPlayerActionPack.ActionType;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EntityPlayerActionPackSerial {
    @Unmodifiable
    private final Map<ActionType, EntityPlayerActionPack.@NonNull Action> actionMap;
    public static final EntityPlayerActionPackSerial EMPTY = new EntityPlayerActionPackSerial();

    private EntityPlayerActionPackSerial() {
        this.actionMap = Map.of();
    }

    public EntityPlayerActionPackSerial(EntityPlayerActionPack actionPack) {
        this.actionMap = ((EntityPlayerActionPackAccessor) actionPack).getActions()
                .entrySet()
                .stream()
                .filter(entry -> !entry.getValue().done)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 从json中反序列化一个对象
     */
    public EntityPlayerActionPackSerial(JsonObject json) {
        EnumMap<ActionType, EntityPlayerActionPack.Action> actions = new EnumMap<>(ActionType.class);
        // 设置假玩家左键
        if (json.has("attack")) {
            JsonObject attack = json.get("attack").getAsJsonObject();
            if (attack.get("continuous").getAsBoolean()) {
                // 左键长按
                actions.put(ActionType.ATTACK, EntityPlayerActionPack.Action.continuous());
            } else {
                // 间隔左键
                int interval = attack.get("interval").getAsInt();
                actions.put(ActionType.ATTACK, EntityPlayerActionPack.Action.interval(interval));
            }
        }
        // 设置假玩家右键
        if (json.has("use")) {
            JsonObject attack = json.get("use").getAsJsonObject();
            if (attack.get("continuous").getAsBoolean()) {
                // 右键长按
                actions.put(ActionType.USE, EntityPlayerActionPack.Action.continuous());
            } else {
                // 间隔右键
                int interval = attack.get("interval").getAsInt();
                actions.put(ActionType.USE, EntityPlayerActionPack.Action.interval(interval));
            }
        }
        this.actionMap = actions;
    }

    /**
     * 设置假玩家动作
     */
    public void startAction(EntityPlayerMPFake fakePlayer) {
        if (this.actionMap.isEmpty()) {
            return;
        }
        EntityPlayerActionPack action = ((ServerPlayerInterface) fakePlayer).getActionPack();
        this.actionMap.forEach(action::start);
    }

    /**
     * （玩家）是否有动作
     */
    public boolean hasAction() {
        return !this.actionMap.isEmpty();
    }

    /**
     * 将动作转换为文本
     */
    public Component getDisplayText(LocalizationKey key) {
        TextJoiner joiner = new TextJoiner();
        // 左键行为
        EntityPlayerActionPack.Action attack = this.actionMap.get(ActionType.ATTACK);
        if (attack != null) {
            joiner.newline(key.then("left_click").translate());
            joiner.enter(() -> getDisplayText(key, attack, joiner));
        }
        // 右键行为
        EntityPlayerActionPack.Action use = this.actionMap.get(ActionType.USE);
        if (use != null) {
            joiner.newline(key.then("right_click").translate());
            joiner.enter(() -> getDisplayText(key, use, joiner));
        }
        return joiner.join();
    }

    private static void getDisplayText(LocalizationKey key, EntityPlayerActionPack.Action action, TextJoiner joiner) {
        if (((ActionAccessor) action).isContinuous()) {
            // 长按
            joiner.newline(key.then("continuous").translate());
        } else {
            // 单击
            joiner.newline(key.then("interval").translate(action.interval));
        }
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        // 左键动作
        EntityPlayerActionPack.Action attack = this.actionMap.get(ActionType.ATTACK);
        if (attack != null) {
            JsonObject attackJson = new JsonObject();
            attackJson.addProperty("interval", attack.interval);
            attackJson.addProperty("continuous", ((ActionAccessor) attack).isContinuous());
            json.add("attack", attackJson);
        }
        // 右键动作
        EntityPlayerActionPack.Action use = this.actionMap.get(ActionType.USE);
        if (use != null) {
            JsonObject useJson = new JsonObject();
            useJson.addProperty("interval", use.interval);
            useJson.addProperty("continuous", ((ActionAccessor) use).isContinuous());
            json.add("use", useJson);
        }
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityPlayerActionPackSerial that = (EntityPlayerActionPackSerial) o;
        if (this.actionMap.isEmpty() && that.actionMap.isEmpty()) {
            return true;
        }
        if (this.actionMap.size() == that.actionMap.size()) {
            for (Map.Entry<ActionType, EntityPlayerActionPack.Action> entry : this.actionMap.entrySet()) {
                ActionType key = entry.getKey();
                EntityPlayerActionPack.Action value = entry.getValue();
                EntityPlayerActionPack.Action action = that.actionMap.get(key);
                if (action == null) {
                    return false;
                }
                if (action == value) {
                    continue;
                }
                if (action.interval != value.interval) {
                    return false;
                }
                if (((ActionAccessor) action).isContinuous() != ((ActionAccessor) value).isContinuous()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (this.actionMap.isEmpty()) {
            return 0;
        }
        return this.actionMap.entrySet().stream().mapToInt(entry -> {
            ActionType key = entry.getKey();
            EntityPlayerActionPack.Action value = entry.getValue();
            ActionAccessor accessor = (ActionAccessor) value;
            return Objects.hash(key.hashCode(), value.interval, Boolean.hashCode(accessor.isContinuous()));
        }).sum();
    }
}
