package io.github.headlesshq.headlessmc.api.settings;

import io.github.headlesshq.headlessmc.api.command.Splitter;
import io.github.headlesshq.headlessmc.api.command.picocli.LegacySplitter;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

final class CollectionSettingBuilderImpl<V, C extends @Unmodifiable Collection<V>>
        extends SettingBuilderImpl<C>
        implements CollectionSettingBuilder<V, C> {
    private final List<V> values = new ArrayList<>();
    private final Supplier<C> collectionFactory;
    private final Class<V> valueType;

    private Splitter splitter = Splitter.defaultSplitter();
    private @Nullable Parser<V> valueParser;
    private char delimiter = ' ';

    @SuppressWarnings("unchecked")
    CollectionSettingBuilderImpl(SettingGroupImpl group, Class<V> valueType, Supplier<C> collectionFactory) {
        super(group, (Class<C>) collectionFactory.get().getClass());
        this.collectionFactory = collectionFactory;
        this.valueType = valueType;
        withValue(collectionFactory.get());
        Parser<V> parser = Parsers.findParser(valueType);
        if (parser != null) {
            this.withValueParser(parser);
        }
    }

    @Override
    public CollectionSettingBuilder<V, C> withDelimiter(char delimiter) {
        this.delimiter = delimiter;
        if (' ' == delimiter) {
            this.splitter = Splitter.defaultSplitter();
        } else {
            this.splitter = new LegacySplitter(delimiter);
        }

        return updateParser();
    }

    @Override
    public CollectionSettingBuilder<V, C> withSplitter(Splitter splitter) {
        this.splitter = splitter;
        return this;
    }

    @Override
    public CollectionSettingBuilder<V, C> withValueParser(Parser<V> valueParser) {
        this.valueParser = valueParser;
        return this;
    }

    @Override
    public CollectionSettingBuilder<V, C> add(V value) {
        values.add(value);
        C collection = collectionFactory.get();
        collection.addAll(values);
        withValue(collection);
        return this;
    }

    @Override
    public <T extends Collection<V>> CollectionSettingBuilder<V, T> withFactory(Supplier<@Unmodifiable T> factory) {
        CollectionSettingBuilderImpl<V, T> result = new CollectionSettingBuilderImpl<>(group, valueType, factory);
        result.withDelimiter(delimiter);
        result.withSplitter(splitter);
        if (valueParser != null) {
            result.withValueParser(valueParser);
        }

        if (withName != null) {
            result.withName(withName);
        }

        if (withDescription != null) {
            result.withDescription(withDescription);
        }

        if (withAliases != null) {
            result.withAlias(withAliases.toArray(new String[0]));
        }

        for (V value : values) {
            result.add(value);
        }

        result.updateParser();
        return result;
    }

    CollectionSettingBuilder<V, C> updateParser() {
        if (valueParser != null) {
            withParser(new CollectionParser<>(collectionFactory, splitter, valueParser, delimiter));
        }

        return this;
    }

}
