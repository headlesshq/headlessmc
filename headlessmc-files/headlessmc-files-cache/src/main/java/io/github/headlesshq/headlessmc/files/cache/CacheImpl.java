package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Default implementation of a {@link Cache}
 * with one {@link CacheSource}.
 * For a multi-level cache see {@link TieredCache}.
 *
 * @param <V> the type of value stored by this cache.
 */
final class CacheImpl<V> extends AbstractCache<V> implements Cache<V> {
    final AtomicReference<@Nullable CacheState<V>> value = new AtomicReference<>();
    final List<CacheStore<V>> stores;
    final CacheSource<V> source;
    final @Nullable V initialValue;

    public CacheImpl(List<CacheStore<V>> stores, CacheSource<V> source, long version, @Nullable V initialValue) {
        super(new Object(), version);
        this.stores = stores;
        this.source = source;
        this.initialValue = initialValue;
    }

    @Override
    public Optional<V> get() throws HeadlessMcException {
        CacheState<V> result = value.get();
        if (result != null) {
            return result.value();
        }

        return getState().value();
    }

    @Override
    CacheState<V> getState() throws HeadlessMcException {
        synchronized (lock) {
            CacheState<V> result = value.get();
            if (result == null) {
                result = new DefaultCacheState<>(null, source.get());
                if (result.value().isEmpty() && initialValue != null) {
                    //noinspection NullableProblems is okay, Optional.of and null check make it NullMarked
                    result = new DefaultCacheState<>(null, Optional.of(initialValue));
                }

                value.set(result);
                V storeValue = result.value().orElse(null);
                if (storeValue != null) {
                    for (CacheStore<V> store : stores) {
                        store.store(object(storeValue));
                    }
                }
            }

            return result;
        }
    }

    @Override
    public Optional<V> set(V value) throws HeadlessMcException {
        synchronized (lock) {
            Optional<V> previous = getInMemoryValue();
            setState(value);
            return previous;
        }
    }

    @Override
    CacheState<V> setState(V value) throws HeadlessMcException {
        synchronized (lock) {
            CacheState<V> result = new DefaultCacheState<>(null, Optional.of(value));
            this.value.set(result);
            for (CacheStore<V> store : stores) {
                store.store(object(value));
            }

            return result;
        }
    }

    @Override
    public String toString() {
        return "CacheImpl{" +
            "source=" + source +
            ", stores=" + stores +
            ", value=" + value +
            ", version=" + version +
            '}';
    }

    @Override
    public Optional<V> clear() {
        synchronized (lock) {
            Optional<V> previous = getInMemoryValue();
            value.set(null);
            return previous;
        }
    }

    @Override
    public Stream<V> stream() {
        return get().stream();
    }

    @Override
    public Iterator<V> iterator() {
        return get().stream().iterator();
    }

    Optional<V> getInMemoryValue() {
        CacheState<V> state = value.get();
        return state == null ? Optional.empty() : state.value();
    }

}
