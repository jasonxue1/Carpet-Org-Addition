package boat.carpetorgaddition.wheel.text;

import boat.carpetorgaddition.CarpetOrgAddition;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.StringJoiner;

public class LocalizationKey {
    private static final String ROOT = CarpetOrgAddition.MOD_ID;
    private final String key;

    private LocalizationKey(String key) {
        this.key = key;
    }

    public static LocalizationKey of(String key) {
        return new LocalizationKey(ROOT + "." + key);
    }

    public static LocalizationKey literal(String key) {
        return new LocalizationKey(key);
    }

    public LocalizationKey then(String key, String... keys) {
        if (keys.length == 0) {
            return new LocalizationKey(this.key + "." + key);
        }
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(this.key).add(key);
        for (String str : keys) {
            joiner.add(str);
        }
        return new LocalizationKey(joiner.toString());
    }

    public Component translate(Object... args) {
        return TextBuilder.translate(this, args);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalizationKey that = (LocalizationKey) o;
        return Objects.equals(this.key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.key);
    }

    @Override
    public String toString() {
        return this.key;
    }
}
