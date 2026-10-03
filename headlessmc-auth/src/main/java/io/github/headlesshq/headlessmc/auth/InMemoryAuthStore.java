package io.github.headlesshq.headlessmc.auth;

import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class InMemoryAuthStore<T> implements AuthStore<T> {
    private volatile SortedMap<String, T> map = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    @Override
    public synchronized @Unmodifiable SortedMap<String, T> read() throws AuthStoreException {
        return map;
    }

    @Override
    public synchronized void add(String id, T value) {
        map.put(id, value);
    }

    @Override
    public synchronized void save(Map<String, T> values) throws AuthStoreException {
        SortedMap<String, T> map = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        map.putAll(values);
        this.map = map;
    }

    @Override
    public synchronized void remove(String id) throws AuthStoreException {
        map.remove(id);
    }

    @Override
    public synchronized Optional<T> getById(String id) throws AuthStoreException {
        return Optional.ofNullable(map.get(id));
    }

}
