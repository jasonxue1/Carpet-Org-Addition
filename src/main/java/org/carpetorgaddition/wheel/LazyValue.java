package org.carpetorgaddition.wheel;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class LazyValue<T> implements Supplier<T> {
    private final T file;
    private final Consumer<T> task;
    private volatile boolean initialized = false;

    public LazyValue(T value, Consumer<T> task) {
        this.file = value;
        this.task = task;
    }

    @Override
    public T get() {
        if (this.initialized) {
            return this.file;
        }
        synchronized (this) {
            if (this.initialized) {
                return this.file;
            }
            this.task.accept(this.file);
            this.initialized = true;
        }
        return this.file;
    }
}
