package org.carpetorgaddition.client.util;

import net.minecraft.client.KeyMapping;

public class ClientKeyBindingUtils {
    /**
     * @return 指定按键是否被按下
     */
    public static boolean isPressed(KeyMapping keyBinding) {
        if (keyBinding.isUnbound()) {
            return false;
        }
        return keyBinding.isDown();
    }
}
