package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Caches store data in memory.
 * The stored data can be viewed ({@link #get()}, {@link #view(Mapping)}),
 * or modified ({@link #set(Object)}, {@link #modify(Consumer)}, etc.).
 *
 * <h3><a id="stores">Cache Stores</a></h3>
 * Caches may store data in {@link CacheStore}s.
 * This allows to e.g. save the body of an HTTP response
 * in a file. Whenever a <i><a id="modifying">modifying</a></i>
 * operation ({@link #set(Object)}, {@link #modify(Consumer)}, etc.)
 * is called, all {@link CacheStore}s will be updated
 * with the new value.
 *
 * <h3><a id="sources">Cache Sources</a></h3>
 * Some caches may fetch data from a {@link CacheSource}.
 * When a <i><a id="retrieving">retrieving</a></i>
 * operation ({@link #get()}, {@link #modify(Consumer)}, etc.)
 * is called the first time, data is requested from the
 * {@link CacheSource}. The retrieved data is then
 * kept in memory until it is replaced by a <a href="#modifying">modifying</a>
 * operation, or {@link #clear}ed.
 *
 * <h3><a id="tiered">Tiered Caches</a></h3>
 * Some caches may fetch data from a list of {@link Cache}s.
 * The last cache in such a list is the primary cache.
 * The default <a href="#retrieving">retrieving</a> methods
 * request the primary cache first,
 * and only request lower level caches if the
 * top level cache did not yield a value.
 * {@link CacheStore}s of lower level caches are updated
 * when a higher level cache has retrieved a value.
 *
 * <p>It is also possible for consumers to walk up the hierarchy,
 * requesting lower level caches first and only
 * requesting higher level caches if needed.
 * This is especially useful if the {@link CacheSource}s
 * of higher level caches are more costly to request.
 * An example for this could be:
 * <pre>
 * - Level 1: classpath resource
 * - Level 2: file on disk
 * - Level 3: remote URL
 * </pre>
 * In this case we would like to request the local
 * caches first before requesting a remote URL.
 * Walking up the hierarchy can be done via the
 * {@link #iterator()} and {@link #stream()} apis,
 * by {@code break} or {@link Stream#findFirst()},
 * to find the first cache to yield a value.
 *
 * @param <V> the type of value stored by this cache.
 */
public interface Cache<V> extends View<V>, CacheStore<V> {
    Optional<V> set(V value) throws HeadlessMcException;

    Optional<V> modify(Consumer<V> consumer) throws HeadlessMcException;

    Optional<V> modify(UnaryOperator<V> action) throws HeadlessMcException;

    Optional<V> maybeModify(Function<V, Boolean> action) throws HeadlessMcException;

    Optional<V> clear();

    <R> Cache<R> mutableView(Mapping<V, R> mapping, Mapping<R, V> reverseMapping) throws HeadlessMcException;

    long getVersion();

    default CacheObject<V> object(V value) {
        return new CacheObject<>(getVersion(), value);
    }

    @Override
    default void store(CacheObject<V> object) throws HeadlessMcException {
        set(object.object());
    }

}
