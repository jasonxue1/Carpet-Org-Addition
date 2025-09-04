package org.carpetorgaddition.client.util;

import net.minecraft.client.option.KeyBinding;

public class ClientKeyBindingUtils {
    /**
     * @return 指定按键是否被按下
     */
    public static boolean isPressed(KeyBinding keyBinding) {
        if (keyBinding.isUnbound()) {
            return false;
        }
        return keyBinding.isPressed();
    }
}
