package io.github.headlesshq.headlessmc.console.format;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public interface TableBuilder<T> {
    TableBuilder<T> withColumn(String name, Function<T, @Nullable String> value);

    TableBuilder<T> withStringValueOf(String name, Function<T, @Nullable Object> value);

    TableBuilder<T> add(T element);

    TableBuilder<T> addAll(Iterable<T> elements);

    TableBuilder<T> withLength(Function<String, Integer> length);

    String toString();

    // <V> TableBuilder<V> as(Function<V, T> mapping);

    default void log(Consumer<String> console) {
        console.accept(toString());
    }

}
