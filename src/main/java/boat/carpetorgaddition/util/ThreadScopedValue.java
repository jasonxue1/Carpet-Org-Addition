package boat.carpetorgaddition.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * ThreadLocal-backed replacement for ThreadScopedValue.
 */
public final class ThreadScopedValue<T> {
    private final ThreadLocal<T> value = new ThreadLocal<>();

    private ThreadScopedValue() {
    }

    public static <T> ThreadScopedValue<T> newInstance() {
        return new ThreadScopedValue<>();
    }

    public static <T> Carrier where(ThreadScopedValue<T> key, T newValue) {
        return new Carrier().where(key, newValue);
    }

    public T get() {
        return this.value.get();
    }

    public boolean isBound() {
        return this.value.get() != null;
    }

    public T orElse(T other) {
        T current = this.value.get();
        return current == null ? other : current;
    }

    private <R> R callWith(T newValue, Supplier<R> supplier) {
        T oldValue = this.value.get();
        boolean hadValue = oldValue != null;
        this.value.set(newValue);
        try {
            return supplier.get();
        } finally {
            if (hadValue) {
                this.value.set(oldValue);
            } else {
                this.value.remove();
            }
        }
    }

    public static final class Carrier {
        private final List<Binding<?>> bindings = new ArrayList<>();

        private Carrier() {
        }

        public <T> Carrier where(ThreadScopedValue<T> key, T newValue) {
            this.bindings.add(new Binding<>(key, newValue));
            return this;
        }

        public void run(Runnable runnable) {
            this.call(() -> {
                runnable.run();
                return null;
            });
        }

        public <R> R call(Supplier<R> supplier) {
            return this.callAt(0, supplier);
        }

        private <R> R callAt(int index, Supplier<R> supplier) {
            if (index >= this.bindings.size()) {
                return supplier.get();
            }
            Binding<?> binding = this.bindings.get(index);
            return binding.call(() -> this.callAt(index + 1, supplier));
        }
    }

    private record Binding<T>(ThreadScopedValue<T> key, T value) {
        private <R> R call(Supplier<R> supplier) {
            return this.key.callWith(this.value, supplier);
        }
    }
}
