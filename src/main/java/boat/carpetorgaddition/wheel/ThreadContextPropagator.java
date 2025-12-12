package boat.carpetorgaddition.wheel;

import org.jetbrains.annotations.Nullable;

public class ThreadContextPropagator<T> {
    private final ThreadLocal<T> external;
    private final ThreadLocal<T> internal;

    public ThreadContextPropagator(@Nullable T value) {
        this.external = ThreadLocal.withInitial(() -> value);
        this.internal = ThreadLocal.withInitial(() -> value);
    }

    public T getExternal() {
        return this.external.get();
    }

    public void setExternal(T value) {
        this.external.set(value);
    }

    public T getInternal() {
        return this.internal.get();
    }

    public void setInternal(T value) {
        this.internal.set(value);
    }

    public void removeExternal() {
        this.external.remove();
    }

    public void removeInternal() {
        this.internal.remove();
    }
}
