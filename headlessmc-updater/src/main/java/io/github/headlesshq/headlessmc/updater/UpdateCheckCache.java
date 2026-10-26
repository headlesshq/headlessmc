package io.github.headlesshq.headlessmc.updater;

import io.github.headlesshq.headlessmc.files.cache.Cache;

import java.time.Instant;
import java.util.Optional;

/**
 * Stores the latest {@link UpdateCheck} of an {@link Updater} on disk.
 * Created by the {@link UpdateCheckCacheFactory}.
 *
 * @param <V> the type of result stored.
 */
public class UpdateCheckCache<V> {
    private final Cache<UpdateCheck<V>> cache;

    UpdateCheckCache(Cache<UpdateCheck<V>> cache) {
        this.cache = cache;
    }

    /**
     * @return the latest check, or empty if nothing has been cached yet.
     */
    public Optional<UpdateCheck<V>> get() {
        return cache.get();
    }

    /**
     * Stores the result of a check that has just been done and writes it to disk.
     *
     * @param value the result of the check.
     */
    public void set(V value) {
        cache.set(new UpdateCheck<>(Instant.now().toEpochMilli(), value));
    }

}
