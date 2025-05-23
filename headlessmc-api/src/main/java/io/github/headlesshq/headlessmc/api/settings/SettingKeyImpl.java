package io.github.headlesshq.headlessmc.api.settings;

import lombok.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Data
class SettingKeyImpl<V> implements SettingKey<V> {
    private final Class<V> type;
    private final String name;
    private final String description;
    private final List<String> aliases;

    @ToString.Exclude
    @Getter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private final Function<Config, V> defaultValue;

    @ToString.Exclude
    @Getter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private final Function<Config, Parser<V>> parser;

    @Override
    public V getDefaultValue(Config config) {
        return defaultValue.apply(config);
    }

    @Override
    public Parser<V> getParser(Config config) {
        return parser.apply(config);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SettingKey)) return false;
        SettingKey<?> that = (SettingKey<?>) o;
        return Objects.equals(getType(), that.getType()) && Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getName());
    }

}
