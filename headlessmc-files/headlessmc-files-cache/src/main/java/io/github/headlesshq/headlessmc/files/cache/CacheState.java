package io.github.headlesshq.headlessmc.files.cache;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

interface CacheState<V> {
    Optional<V> value();

    @Nullable Object parent();

}
