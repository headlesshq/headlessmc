package io.github.headlesshq.headlessmc.api.newapi.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
class SettingImpl<V> implements Setting<V> {
    private final Object lock = new Object();

    private final String name;
    private final int id;
    private final String description;
    private final Supplier<V> defaultValueSupplier;
    private final Class<V> type;
    private final Parser<V> parser;

    private volatile V defaultValue;
    private volatile V userValue;
    private volatile V value;

    private void init() {
        synchronized (lock) {
            if (this.defaultValue == null) {
                this.defaultValue = this.defaultValueSupplier.get();
                if (this.value == null) {
                    this.value = this.defaultValue;
                }
            }
        }
    }

    @Override
    public V get() {
        synchronized (lock) {
            if (userValue != null) {
                return userValue;
            }

            String systemProperty = System.getProperty(name);
            if (systemProperty != null) {
                return getParser().parse(systemProperty);
            }

            init();
            return this.value;
        }
    }

    @Override
    public void set(SetContext context, V value) {
        synchronized (lock) {
            init();
            this.value = value;
            if (context == SetContext.USER) {
                this.userValue = value;
            }
        }
    }

    @Override
    public V getDefault() {
        init();
        return defaultValue;
    }

}
