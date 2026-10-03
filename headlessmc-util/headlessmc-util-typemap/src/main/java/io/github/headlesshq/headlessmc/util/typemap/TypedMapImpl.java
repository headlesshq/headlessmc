package io.github.headlesshq.headlessmc.util.typemap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link TypedMap}.
 * Backed by a {@link ConcurrentHashMap}.
 */
@SuppressWarnings("unchecked")
public final class TypedMapImpl implements TypedMap {
    private final Map<Key<?>, Object> map = new ConcurrentHashMap<>();

    @Override
    public @Nullable <V> V get(Key<V> key) {
        return (V) map.get(key);
    }

    @Override
    public <V> V get(Key<V> key, V defaultValue) {
        V result = get(key);
        return result == null ? defaultValue : result;
    }

    @Override
    public @Nullable <V> V put(Key<V> key, V value) {
        return (V) map.put(key, value);
    }

    @Override
    public @Nullable <V> V remove(Key<V> key) {
        return (V) map.remove(key);
    }

    @Override
    public @NonNull Iterator<Entry<?>> iterator() {
        return map.entrySet()
            .stream()
            .map(EntryImpl::of)
            .iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TypedMapImpl other = (TypedMapImpl) o;
        return Objects.equals(map, other.map);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(map);
    }

    @Override
    public String toString() {
        return map.toString();
    }

    private record EntryImpl<V>(Key<V> key, V value) implements Entry<V> {
        @SuppressWarnings("rawtypes")
        public static Entry<?> of(Map.Entry<Key<?>, Object> mapEntry) {
            return new EntryImpl(mapEntry.getKey(), mapEntry.getValue());
        }
    }

}
