package io.github.headlesshq.headlessmc.api.settings;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

@Setter
@Accessors(fluent = true)
public final class SettingBuilderImpl<V> implements SettingBuilder<V> {
    private final SettingGroupImpl group;
    private final Class<V> withType;

    private String withName;
    private String withDescription;
    private List<String> withAliases;
    private Function<Config, V> withValue;
    private Function<Config, Parser<V>> withParser;

    SettingBuilderImpl(SettingGroupImpl group, Class<V> type) {
        this.group = group;
        this.withType = type;
        Parser<V> parser = Parsers.findParser(type);
        if (parser != null) {
            this.withParser = c -> parser;
        }
    }

    public SettingBuilder<V> withValue(V value) {
        this.withValue = c -> value;
        return this;
    }

    @Override
    public SettingBuilder<V> withValue(Function<Config, V> value) {
        this.withValue = requireNonNull(value);
        return this;
    }

    @Override
    public SettingBuilder<V> withParser(Parser<V> parser) {
        this.withParser = c -> parser;
        return this;
    }

    @Override
    public SettingBuilder<V> withParser(Function<Config, Parser<V>> parser) {
        this.withParser = requireNonNull(parser);
        return this;
    }

    public SettingBuilder<V> withAlias(String... aliases) {
        List<String> newAliases = new ArrayList<>();
        if (this.withAliases != null) {
            newAliases.addAll(this.withAliases);
        }

        newAliases.addAll(Arrays.asList(aliases));
        this.withAliases = newAliases;
        return this;
    }

    @Override
    public SettingKey<@Nullable V> nullable() {
        List<String> aliases = this.withAliases == null ? emptyList() : unmodifiableList(this.withAliases);
        Function<Config, V> value = this.withValue == null ? c -> null : this.withValue;
        return group.add(new SettingKeyImpl<>(
                requireNonNull(withType, "withType not called"),
                requireNonNull(withName, "withName not called"),
                requireNonNull(withDescription, "withDescription not called"),
                aliases,
                value,
                requireNonNull(withParser, "withParser not called")
        ));
    }

    public SettingKey<V> build() {
        List<String> aliases = this.withAliases == null ? emptyList() : unmodifiableList(this.withAliases);
        return group.add(new SettingKeyImpl<>(
                requireNonNull(withType, "withType not called"),
                requireNonNull(withName, "withName not called"),
                requireNonNull(withDescription, "withDescription not called"),
                aliases,
                requireNonNull(withValue, "withValue not called"),
                requireNonNull(withParser, "withParser not called")
        ));
    }

}
