package io.github.headlesshq.headlessmc.api.settings;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// TODO: load
// TODO: should get a setting group so it can parse all settings in that group beforehand
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ConfigImpl implements Config {
    private final Map<SettingKey<?>, Setting<?>> cachedValues;
    private final Path configPath;
    private final Path applicationPath;
    private final Properties properties;

    @Override
    public <V> V get(SettingKey<V> key) {
        return getSetting(key).getWithFallbackToDefault();
    }

    @Override
    public <V> @Nullable V get(NullableSettingKey<V> key) {
        return getSetting(key).getWithFallbackToDefault();
    }

    @Override
    public <V> void set(Scope scope, SettingKey<V> key, V value) {
        Setting<V> setting = getSetting(key);
        setting.values.put(scope, value == null ? null : Optional.of(value));
        if (scope.isWrittenToConfig()) {
            onChange(setting, value);
        }
    }

    @Override
    public <V> @Nullable V getRaw(SettingKey<V> key) {
        return getSetting(key).get();
    }

    @Override
    public void bulkUpdate(Consumer<Config> action) {
        boolean[] changed = new boolean[] { false };
        action.accept(new ConfigImpl(cachedValues, configPath, applicationPath, properties) {
            @Override
            void save() {
                changed[0] = true;
            }
        });

        if (changed[0]) {
            this.save();
        }
    }

    @Override
    public Path getConfigPath() {
        return configPath;
    }

    @Override
    public Path getApplicationPath() {
        return applicationPath;
    }

    private <V> void onChange(Setting<V> setting, @Nullable Object value) {
        if (value == null) {
            properties.remove(setting.key.getName().toLowerCase(Locale.ENGLISH));
            save();
            return;
        }

        String string = setting.key.getParser(this).toParseableString(setting.key.getType().cast(value));
        Object previous = properties.put(setting.key.getName().toLowerCase(Locale.ENGLISH), string);
        if (!Objects.equals(previous, string)) {
            save();
        }
    }

    void save() {
        try (OutputStream out = Files.newOutputStream(configPath)) {
            properties.store(out, null);
        } catch (IOException e) {
            throw new IOError(new IOException("Failed to save config", e));
        }
    }

    @SuppressWarnings("unchecked")
    private <V> Setting<V> getSetting(SettingKey<V> key) {
        Setting<?> setting = cachedValues.computeIfAbsent(key, Setting::new);
        // we could be a bit more lenient here to allow SettingKeys of super types, but that is a slippery slope
        if (!key.getType().equals(setting.key.getType())) {
            throw new IllegalArgumentException("Setting key " + key + " is not of type " + setting.key.getType() + ", present key: " + setting.key);
        }

        return (Setting<V>) setting;
    }

    static ConfigImpl load(Path applicationPath, Path configPath) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
        }

        return new ConfigImpl(new ConcurrentHashMap<>(), applicationPath, configPath, properties);
    }

    @RequiredArgsConstructor
    private final class Setting<V> {
        private final EnumMap<Scope, Optional<V>> values = new EnumMap<>(Scope.class);
        private final SettingKey<V> key;

        private volatile String systemProperty;
        private volatile V parsedSystemProperty;

        private volatile V defaultValue;

        public V getWithFallbackToDefault() {
            synchronized (values) {
                V value = get();
                if (value == null) {
                    if (defaultValue == null) {
                        defaultValue = key.getDefaultValue(ConfigImpl.this);
                    }

                    value = defaultValue;
                }

                return value;
            }
        }

        public @Nullable V get() {
            synchronized (values) {
                Optional<V> value = values.get(Scope.USER);
                if (value != null) {
                    return value.orElse(null);
                }

                value = values.get(Scope.USER_APPLICATION);
                if (value != null) {
                    return value.orElse(null);
                }

                String systemProperty = System.getProperty(key.getName());
                if (systemProperty != null) {
                    if (!systemProperty.equals(this.systemProperty)) {
                        this.systemProperty = systemProperty;
                        this.parsedSystemProperty = key.getParser(ConfigImpl.this).parse(systemProperty);
                    }

                    return parsedSystemProperty;
                }

                return Optional.ofNullable(values.get(Scope.APPLICATION))
                        .map(Optional::ofNullable)
                        .orElseGet(() -> Optional.ofNullable(values.get(Scope.USER)))
                        .orElse(Optional.empty())
                        .orElse(null);
            }
        }


    }

}
