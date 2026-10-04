package io.github.headlesshq.headlessmc.files.cache;

/**
 * This class helps with constructing instances of {@link Cache}.
 *
 * @param <V> the type of value stored by the cache to build.
 */
public interface CacheBuilder<V> {
    /**
     * Sets the initial value to fill the cache with if the source is not available.
     *
     * @param value the initial value for the cache.
     * @return this builder.
     */
    CacheBuilder<V> withInitialValue(V value);

    /**
     * Sets the {@link Cache#getVersion()} for the cache to build.
     *
     * @param version the version of the cache.
     * @return this builder.
     */
    // Eventually when we need them we also need migrations to upgrade cache formats
    CacheBuilder<V> withVersion(long version);

    /**
     * Adds a {@link CacheSource} to the cache.
     * If more than one {@link CacheSource} is added,
     * the returned cache will be
     * <a href="Cache.html#tiered"><i>tiered</i></a>.
     *
     * @apiNote special care needs to be taken with the order
     *          of {@code withSource}, {@link #withStore}
     *          and {@link #withSourceStore},
     *          because in a tiered cache, higher-level stores
     *          will not receive values from higher-level sources.
     * @param source the source to add to the Cache.
     * @return this builder.
     */
    CacheBuilder<V> withSource(CacheSource<V> source);

    /**
     * Adds a {@link CacheStore} to the cache.
     *
     * @apiNote special care needs to be taken with the order
     *          of {@link #withSource}, {@code withStore}
     *          and {@link #withSourceStore},
     *          because in a tiered cache, higher-level stores
     *          will not receive values from higher-level sources.
     * @param store the store to add to the Cache.
     * @return this builder.
     */
    CacheBuilder<V> withStore(CacheStore<V> store);

    /**
     * Adds a {@link CacheStore} and a {@link CacheSource}
     * on the same level to the cache.
     * Behaves similarly to a call to {@link #withStore} followed
     * by a call to {@link #withSource}.
     *
     * @apiNote special care needs to be taken with the order
     *          of {@link #withSource}, {@link #withStore}
     *          and {@code withSourceStore},
     *          because in a tiered cache, higher-level stores
     *          will not receive values from higher-level sources.
     * @param sourceStore the store/source to add to the Cache.
     * @return this builder.
     */
    <C extends CacheSource<V> & CacheStore<V>> CacheBuilder<V> withSourceStore(C sourceStore);

    /**
     * @return a new {@link Cache} instance.
     */
    Cache<V> build();

    /**
     * Creates a default {@link CacheBuilder} implementation,
     * which can instantiate caches and tiered caches.
     *
     * @return the default {@link CacheBuilder} implementation.
     * @param <V> the type of value stored by the cache to build.
     */
    static <V> CacheBuilder<V> create() {
        return new CacheBuilderImpl<>();
    }

}
