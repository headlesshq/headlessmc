package io.github.headlesshq.headlessmc.exceptions;

@FunctionalInterface
public interface ExFunction<T, R> {
    R apply(T t) throws Exception;

    @FunctionalInterface
    interface Generic<T, R, E extends Exception> extends ExFunction<T, R> {
        @Override
        R apply(T t) throws E;
    }

}
