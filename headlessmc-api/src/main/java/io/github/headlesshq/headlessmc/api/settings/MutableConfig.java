package io.github.headlesshq.headlessmc.api.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface MutableConfig {
    <V> void set(Scope scope, SettingKey<V> key, V value);

    <V> void set(Scope scope, NullableSettingKey<V> key, @Nullable V value);

    void bulkUpdate(Consumer<MutableConfig> action);

    @Getter
    @RequiredArgsConstructor
    enum Scope {
        USER(true),
        CONFIG(true),
        USER_APPLICATION(false),
        APPLICATION(false);

        private final boolean writtenToConfig;
    }

}
