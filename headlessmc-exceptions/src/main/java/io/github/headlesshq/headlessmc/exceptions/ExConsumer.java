package io.github.headlesshq.headlessmc.exceptions;

/**
 * A {@link java.util.function.Consumer} that can throw Exceptions.
 *
 * @param <T> the type of the input to the operation
 */
@FunctionalInterface
public interface ExConsumer<T> {
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws Exception if something goes wrong.
     */
    void accept(T t) throws Exception;

    @FunctionalInterface
    interface Generic<T, E extends Exception> extends ExConsumer<T> {
        @Override
        void accept(T t) throws E;
    }

}
