package io.github.headlesshq.headlessmc.util.typemap;

import org.jspecify.annotations.Nullable;

/**
 * A heterogeneous map that associates strongly typed {@link Key Keys} with
 * values.
 * <p>
 * Unlike a regular {@link java.util.Map}, each key carries its associated value
 * type, allowing values to be retrieved without explicit casts.
 * <p>
 * Implementations may define their own thread-safety guarantees.
 */
public interface TypedMap extends Iterable<TypedMap.Entry<?>> {
    /**
     * Returns the value associated with the given key.
     *
     * @param key the key
     * @param <V> the value type
     * @return the stored value, or {@code null} if no value is associated with
     *         the key
     */
    <V> @Nullable V get(Key<V> key);

    /**
     * Returns the value associated with the given key, or the supplied default
     * value if no value is present.
     *
     * @param key the key
     * @param defaultValue the value to return if the key is not present
     * @param <V> the value type
     * @return the stored value, or {@code defaultValue} if none exists
     */
    <V> V get(Key<V> key, V defaultValue);

    /**
     * Associates the given value with the specified key.
     *
     * @param key the key
     * @param value the value to store
     * @param <V> the value type
     * @return the previous value associated with the key, or {@code null} if
     *         there was none
     */
    <V> @Nullable V put(Key<V> key, V value);

    /**
     * Removes the value associated with the given key.
     *
     * @param key the key
     * @param <V> the value type
     * @return the removed value, or {@code null} if no value was associated
     *         with the key
     */
    <V> @Nullable V remove(Key<V> key);

    /**
     * A key-value pair contained in a {@link TypedMap}.
     *
     * @param <V> the value type
     */
    interface Entry<V> {
        /**
         * Returns the entry's key.
         *
         * @return the key
         */
        Key<V> key();

        /**
         * Returns the entry's value.
         *
         * @return the value
         */
        V value();
    }

}
