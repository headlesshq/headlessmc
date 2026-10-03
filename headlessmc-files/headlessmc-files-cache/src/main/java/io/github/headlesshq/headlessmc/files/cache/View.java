package io.github.headlesshq.headlessmc.files.cache;

import io.github.headlesshq.headlessmc.exceptions.HeadlessMcException;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * A View provides data held by a cache.
 * It may also provide a transformation of such data,
 * via a {@link Mapping}.
 *
 * @param <V> the type of the value this view provides.
 */
public interface View<V> extends Iterable<V>, CacheSource<V> {
    <R> View<R> view(Mapping<V, R> mapping);

    Stream<V> stream() throws HeadlessMcException;

    @Override
    Iterator<V> iterator() throws HeadlessMcException;

    @FunctionalInterface
    interface Mapping<V, R> {
        R map(V value, @Nullable R previous) throws HeadlessMcException;
    }

}
