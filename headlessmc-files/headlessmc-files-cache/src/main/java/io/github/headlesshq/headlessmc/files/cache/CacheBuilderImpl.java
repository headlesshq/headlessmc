package io.github.headlesshq.headlessmc.files.cache;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CacheBuilderImpl<V> implements CacheBuilder<V> {
    private final List<SourceStoreContainer<V>> containers = new ArrayList<>();
    private CacheSource<V> source = CacheSource.empty();
    private @Nullable V initialValue;
    private long version;
    private int sources;

    @Override
    public CacheBuilder<V> withInitialValue(V value) {
        this.initialValue = value;
        return this;
    }

    @Override
    public CacheBuilder<V> withVersion(long version) {
        this.version = version;
        return this;
    }

    @Override
    public CacheBuilder<V> withSource(CacheSource<V> source) {
        containers.add(new SourceStoreContainer<>(Optional.of(source), Optional.empty()));
        this.source = source;
        sources++;
        return this;
    }

    @Override
    public CacheBuilder<V> withStore(CacheStore<V> store) {
        containers.add(new SourceStoreContainer<>(Optional.empty(), Optional.of(store)));
        return this;
    }

    @Override
    public <C extends CacheSource<V> & CacheStore<V>> CacheBuilder<V> withSourceStore(C sourceStore) {
        containers.add(new SourceStoreContainer<>(Optional.of(sourceStore), Optional.of(sourceStore)));
        this.source = sourceStore;
        sources++;
        return this;
    }

    @Override
    public Cache<V> build() {
        if (sources > 1) {
            List<Cache<V>> caches = containers.stream()
                .map(container -> {
                    List<CacheStore<V>> stores = container.store.stream().toList();
                    CacheSource<V> source = container.source.orElseGet(CacheSource::empty);
                    return (Cache<V>) new CacheImpl<>(stores, source, version, initialValue);
                }).toList();

            return new TieredCache<>(caches, version);
        }

        List<CacheStore<V>> stores = containers.stream()
            .map(SourceStoreContainer::store)
            .flatMap(Optional::stream)
            .toList();

        //noinspection NullableProblems // JSpecify bug
        return new CacheImpl<>(stores, source, version, initialValue);
    }

    private record SourceStoreContainer<V>(Optional<CacheSource<V>> source, Optional<CacheStore<V>> store) {}

}
