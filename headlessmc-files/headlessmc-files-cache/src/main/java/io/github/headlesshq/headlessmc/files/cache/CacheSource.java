package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.util.Optional;

@FunctionalInterface
public interface CacheSource<V> extends CacheFunction {
    Optional<V> get() throws HeadlessMcException;

    static <V> CacheSource<V> empty() {
        return Optional::empty;
    }

}
