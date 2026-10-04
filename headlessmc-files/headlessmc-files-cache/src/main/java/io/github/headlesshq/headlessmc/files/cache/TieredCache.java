package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Implementation of a <a href="Cache.html#tiered"><i>tiered</i></a> cache.
 *
 * @param <V> the type of value stored by this cache.
 */
final class TieredCache<V> extends AbstractCache<V> implements Cache<V> {
    private final AtomicReference<@Nullable TieredCacheState<V>> state = new AtomicReference<>();
    private final List<Cache<V>> caches;

    TieredCache(List<Cache<V>> caches, long version) {
        super(new Object(), version);
        if (caches.isEmpty()) {
            throw new IllegalArgumentException("Caches can't be empty");
        }

        this.caches = caches;
    }

    @Override
    public Optional<V> get() throws HeadlessMcException {
        TieredCacheState<V> state = this.state.get();
        if (state != null && state.complete) {
            return state.state.value();
        }

        return getState().value();
    }

    @Override
    public Optional<V> set(V value) throws HeadlessMcException {
        synchronized (lock) {
            Optional<V> before = Optional.ofNullable(state.get())
                    .map(TieredCacheState::state)
                    .flatMap(CacheState::value);

            setState(value);
            return before;
        }
    }

    @Override
    public Optional<V> clear() {
        synchronized (lock) {
            Optional<V> before = Optional.ofNullable(state.get())
                    .map(TieredCacheState::state)
                    .flatMap(CacheState::value);

            caches.forEach(Cache::clear);
            this.state.set(null);
            return before;
        }
    }

    @Override
    public Stream<V> stream() {
        TieredCacheState<V> state = this.state.get();
        if (state != null && state.complete) {
            return state.state.value().stream();
        }

        synchronized (lock) {
            // check after locking, some other thread may have changed this
            state = this.state.get();
            if (state != null && state.complete) {
                return state.state.value().stream();
            }

            return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED),
                false
            );
        }
    }

    @Override
    public Iterator<V> iterator() {
        return new TieredCacheIterator();
    }

    @Override
    CacheState<V> getState() throws HeadlessMcException {
        synchronized (lock) {
            for (int i = caches.size() - 1; i >= 0; i--) {
                Cache<V> cache = caches.get(i);
                // TODO: need to actually get state of highest cache
                Optional<V> value = cache.get();
                if (value.isPresent()) {
                    TieredCacheState<V> result = TieredCacheState.completed(i + 1, value.get());
                    this.state.set(result);
                    for (int j = 0; j < i; j++) {
                        caches.get(j).set(value.get());
                    }

                    return result;
                }
            }

            TieredCacheState<V> result = TieredCacheState.completed(0, null);
            this.state.set(result);
            return result;
        }
    }

    @Override
    CacheState<V> setState(V value) throws HeadlessMcException {
        for (Cache<V> cache : caches) {
            cache.set(value);
        }

        TieredCacheState<V> result = new TieredCacheState<>(
                null,
                caches.size(),
                true,
                new DefaultCacheState<>(null, Optional.of(value))
        );

        this.state.set(result);
        return result;
    }

    @Override
    public String toString() {
        return "TieredCache{" +
            "version=" + version +
            ", caches=" + caches +
            ", state=" + state +
            '}';
    }

    record TieredCacheState<V>(
            @Nullable Object parent,
            int index,
            boolean complete,
            CacheState<V> state
    ) implements CacheState<V> {
        @Override
        public Optional<V> value() {
            return state.value();
        }

        @Override
        public @Nullable Object parent() {
            return parent;
        }

        static <V> TieredCacheState<V> completed(int index, @Nullable V value) {
            //noinspection NullableProblems
            return new TieredCacheState<>(null, index, true, new DefaultCacheState<>(null, Optional.ofNullable(value)));
        }
    }

    class TieredCacheIterator implements Iterator<V> {
        boolean fresh = true;
        boolean completed;
        @Nullable V next;

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }

            if (completed) {
                return false;
            }

            synchronized (lock) {
                TieredCacheState<V> state = TieredCache.this.state.get();
                if (state != null && state.complete) {
                    completed = true;
                    if (state.value().isPresent()) {
                        next = state.value().get();
                    } else {
                        return false;
                    }
                }

                if (fresh) {
                    fresh = false;
                    if (state != null && state.value().isPresent()) {
                        next = state.value().get();
                        return true;
                    }
                }

                int index = state == null ? 0 : state.index;
                if (index >= caches.size()) {
                    return false;
                }

                for (int i = index; i < caches.size(); i++) {
                    Cache<V> cache = caches.get(i);
                    Optional<V> value = cache.get();
                    if (value.isPresent()) {
                        TieredCacheState<V> result = new TieredCacheState<>(
                            null, i + 1, i == caches.size() - 1, new DefaultCacheState<>(null, value)
                        );

                        TieredCache.this.state.set(result);
                        for (int j = 0; j < i; j++) {
                            caches.get(j).set(value.get());
                        }

                        if (result.complete) {
                            completed = true;
                        }

                        next = value.get();
                        return true;
                    }
                }

                TieredCacheState<V> result = TieredCacheState.completed(0, null);
                TieredCache.this.state.set(result);
                completed = true;
                return false;
            }
        }

        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException("next() called without a check to hasNext()");
            }

            V result = Objects.requireNonNull(next, "next null even though hasNext has been called, " +
                    "has Iterator been shared between threads?");
            next = null;
            return result;
        }
    }

}
