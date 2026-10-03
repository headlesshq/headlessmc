package io.github.headlesshq.headlessmc.auth;

import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Stream;

/**
 * Represents storage for authentication information.
 * This can e.g. be a file for a database, of e.g. accounts.
 *
 * @param <T> the type of authentication information to store.
 */
public interface AuthStore<T> {
    @Unmodifiable
    SortedMap<String, T> read() throws AuthStoreException;

    void add(String id, T value) throws AuthStoreException;

    void save(Map<String, T> values) throws AuthStoreException;

    void remove(String id) throws AuthStoreException;

    Optional<T> getById(String id) throws AuthStoreException;

    default Stream<T> stream() throws AuthStoreException {
        return read().sequencedValues().stream();
    }

}
