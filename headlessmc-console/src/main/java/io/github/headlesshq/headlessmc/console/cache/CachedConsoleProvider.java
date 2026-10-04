package io.github.headlesshq.headlessmc.console.cache;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.ConsoleProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CachedConsoleProvider implements ConsoleProvider {
    private final ConsoleCachingService cache;

    @Override
    public Console get() throws ConsoleException {
        return cache.get()
            .map(CachedConsole::get)
            .orElseThrow(() -> new ConsoleException("No console cached"));
    }

    @Override
    public int sort() {
        return SORT_CACHE;
    }

}
