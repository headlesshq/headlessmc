package io.github.headlesshq.headlessmc.console.cache;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class ConsoleCachingService {
    private final AtomicReference<@Nullable CachedConsole> state = new AtomicReference<>();

    public synchronized CachedConsole cache(Console console) {
        if (state.get() != null) {
            throw new IllegalStateException("Cannot re-cache console");
        }

        @SuppressWarnings("resource") // closed by handle we hand out, so consumer has to handle
        CachedConsole consoleToCache = console.cache();
        CachedConsole handle = new CachedConsole() {
            @Override
            public Console get() {
                return consoleToCache.get();
            }

            @Override
            public void close() throws ConsoleException {
                state.set(null); // compare?
                consoleToCache.close();
            }
        };

        state.set(handle);
        return handle;
    }

    public Optional<CachedConsole> get() {
        return Optional.ofNullable(state.get());
    }

}
