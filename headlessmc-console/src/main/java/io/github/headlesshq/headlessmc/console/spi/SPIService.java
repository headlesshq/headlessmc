package io.github.headlesshq.headlessmc.console.spi;

import io.github.headlesshq.headlessmc.console.ConsoleException;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SPIService<T, C extends ConsoleSPI<T, C>> {
    private final List<C> providers;
    private final String name;

    public T get() {
        ConsoleException exception = new ConsoleException("Failed to open " + name);
        for (C provider : providers) {
            try {
                return provider.get();
            } catch (ConsoleException e) {
                exception.addSuppressed(e);
            }
        }

        throw exception;
    }

}
