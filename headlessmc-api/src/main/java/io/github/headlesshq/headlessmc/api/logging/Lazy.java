package io.github.headlesshq.headlessmc.api.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@Getter
@Setter
@AllArgsConstructor
final class Lazy<T> implements Supplier<T> {
    private final Object lock = new Object();
    private volatile Supplier<T> supplier;
    private volatile @Nullable T value;

    @Override
    public T get() {
        T value = this.value;
        if (value == null) {
            synchronized (lock) {
                value = this.value;
                if (value == null) {
                    value = supplier.get();
                    this.value = value;
                }
            }
        }

        return value;
    }

}
