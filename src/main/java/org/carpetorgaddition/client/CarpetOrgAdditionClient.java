package org.carpetorgaddition.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class CarpetOrgAdditionClient implements ClientModInitializer {
    /**
     * 清除高亮路径点的按键绑定
     */
    public static final KeyBinding CLEAR_WAYPOINT = new KeyBinding("carpet.client.key.keyboard.waypoint.clear", InputUtil.UNKNOWN_KEY.getCode(), KeyBinding.Category.MISC);

    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        CarpetOrgAdditionClientRegister.register();
    }
}
