package io.github.headlesshq.headlessmc.api.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

// groups?
public interface Config {
    <V> V get(SettingKey<V> key);

    <V> @Nullable V get(NullableSettingKey<V> key);

    <V> void set(Scope scope, SettingKey<V> key, V value);

    Path getConfigPath();

    Path getApplicationPath();

    <V> @Nullable V getRaw(SettingKey<V> key);

    void bulkUpdate(Consumer<Config> action);

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
