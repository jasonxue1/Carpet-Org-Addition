package boat.carpetorgaddition.client;

import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;

public class CarpetOrgAdditionClient implements ClientModInitializer {
    /**
     * 清除高亮路径点的按键绑定
     */
    public static final KeyMapping CLEAR_WAYPOINT = new KeyMapping(LocalizationKeys.Keyboard.WAYPOINT.then("clear").toString(), InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);

    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        CarpetOrgAdditionClientRegister.register();
    }
}
