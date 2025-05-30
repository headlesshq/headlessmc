package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.command.Splitter;

import java.util.Collection;
import java.util.function.Supplier;

public interface CollectionSettingBuilder<V, C extends Collection<V>> extends SettingBuilder<C> {
    CollectionSettingBuilder<V, C> withDelimiter(char delimiter);

    CollectionSettingBuilder<V, C> withSplitter(Splitter splitter);

    CollectionSettingBuilder<V, C> withValueParser(Parser<V> valueParser);

    CollectionSettingBuilder<V, C> add(V value);

    <T extends Collection<V>> CollectionSettingBuilder<V, T> withFactory(Supplier<T> factory);

}
