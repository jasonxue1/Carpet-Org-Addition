package boat.carpetorgaddition.config;

import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

public class HiddenFunctionConfig extends AbstractConfig<JsonPrimitive> {
    private boolean enable;
    private boolean shouldBeSaved = false;

    protected HiddenFunctionConfig(GlobalConfigs globalConfigs) {
        super(globalConfigs);
    }

    @Override
    public void load(@Nullable JsonPrimitive json) {
        if (json == null) {
            this.enable = false;
        } else {
            this.shouldBeSaved = true;
            this.enable = json.getAsBoolean();
        }
    }

    @Override
    public String getKey() {
        return "enableHiddenFunction";
    }

    @Override
    public boolean shouldBeSaved() {
        return this.shouldBeSaved;
    }

    @Override
    public JsonPrimitive getJsonValue() {
        return new JsonPrimitive(this.enable);
    }

    public boolean isEnable() {
        return this.enable;
    }
}
