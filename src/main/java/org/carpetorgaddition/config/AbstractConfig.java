package org.carpetorgaddition.config;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractConfig<T extends JsonElement> {
    protected AbstractConfig() {
    }

    public abstract void load(@Nullable T json);

    /**
     * @return 配置的json名称
     */
    public abstract String getKey();

    /**
     * @return 配置的json元素
     */
    public abstract T getJsonValue();

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj != null && this.getClass() == obj.getClass());
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
