package io.github.headlesshq.headlessmc.exceptions;

@FunctionalInterface
public interface ExRunnable {
    void run() throws Exception;

    interface Generic<E extends Exception> extends ExRunnable {
        @Override
        void run() throws E;
    }

}
