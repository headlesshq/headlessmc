package io.github.headlesshq.headlessmc.files.cache;

/**
 * Marker interface for {@link CacheSource} and {@link CacheStore}
 * and other APIs a {@link Cache} will access that can be handled
 * by an {@link CacheExceptionHandler}.
 *
 * @see CacheStore
 * @see CacheSource
 * @see CacheExceptionHandler
 */
public interface CacheFunction {
    /**
     * CacheFunctions should define a toString implementation,
     * so they can be logged by {@link CacheExceptionHandler}.
     *
     * @return a string containing information
     * (e.g. File, URL etc.) for this CacheFunction.
     */
    String toString();

}
