package io.github.headlesshq.headlessmc.files.cache;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

record DefaultCacheState<V>(@Nullable Object parent, Optional<V> value) implements CacheState<V> {

}
