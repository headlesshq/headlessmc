package io.github.headlesshq.headlessmc.api.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

// groups?
// TODO: maybe a handle for settings that are used often?
public interface Config {
    <V> V get(SettingKey<@NotNull V> key);

    <V> @Nullable V get(NullableSettingKey<V> key);

    <V> void set(Scope scope, SettingKey<V> key, V value);

    <V> void set(Scope scope, NullableSettingKey<V> key, @Nullable V value);

    Path getConfigPath();

    Path getApplicationPath();

    <V> @Nullable V getRaw(SettingKey<V> key);

    void bulkUpdate(Consumer<Config> action);

    default <V> V get(NullableSettingKey<V> key, Supplier<V> defaultValue) {
        V result = get(key);
        return result == null ? defaultValue.get() : result;
    }

    static Config load(Path applicationPath, Path configPath) throws IOException {
        return ConfigImpl.load(applicationPath, configPath);
    }

    // TODO: config purely based on system properties?
    // TODO: for that distinction between mutable config and just getter config?

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
