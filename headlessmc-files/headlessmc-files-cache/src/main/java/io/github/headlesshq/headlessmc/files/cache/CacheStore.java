package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

@FunctionalInterface
public interface CacheStore<V> extends CacheFunction {
    void store(CacheObject<V> object) throws HeadlessMcException;

}
