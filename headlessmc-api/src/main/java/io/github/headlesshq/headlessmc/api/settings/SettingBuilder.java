package io.github.headlesshq.headlessmc.api.settings;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface SettingBuilder<V> {
    SettingBuilder<V> withName(String name);

    SettingBuilder<V> withDescription(String description);

    SettingBuilder<V> withValue(V value);

    SettingBuilder<V> withValue(Function<Config, V> value);

    SettingBuilder<V> withParser(Parser<V> parser);

    SettingBuilder<V> withParser(Function<Config, Parser<V>> parser);

    SettingBuilder<V> withAlias(String... aliases);

    SettingKey<@Nullable V> nullable();

    SettingKey<V> build();

}
