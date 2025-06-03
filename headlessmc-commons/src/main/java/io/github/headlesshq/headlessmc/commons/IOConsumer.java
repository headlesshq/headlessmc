package io.github.headlesshq.headlessmc.commons;

import java.io.IOException;

@FunctionalInterface
public interface IOConsumer<T> {
    void accept(T value) throws IOException;

}
