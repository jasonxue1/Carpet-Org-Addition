package org.carpetorgaddition.client.util;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class ClientKeyBindingUtils {
    /**
     * @return 指定按键是否被按下
     */
    public static boolean isPressed(KeyBinding keyBinding) {
        if (keyBinding.isUnbound()) {
            return false;
        }
        InputUtil.Key boundKeyOf = KeyBindingHelper.getBoundKeyOf(keyBinding);
        return InputUtil.isKeyPressed(ClientUtils.getClient().getWindow().getHandle(), boundKeyOf.getCode());
    }
}
