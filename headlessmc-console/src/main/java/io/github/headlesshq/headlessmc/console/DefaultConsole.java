package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.output.SecretService;
import io.github.headlesshq.headlessmc.console.spi.SPIService;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Delegates to the first available {@link ConsoleProvider}
 * and removes secrets from everything written with the {@link SecretService}.
 */
@RequiredArgsConstructor
public class DefaultConsole implements Console {
    private final List<ConsoleProvider> providers;
    private final SecretService secretService;

    @Override
    public void write(String message) {
        getConsole().write(message);
    }

    @Override
    public void write(String message, String nextLine) {
        getConsole().write(message, nextLine);
    }

    @Override
    public String read() {
        return getConsole().read();
    }

    @Override
    public Password readPassword() {
        return getConsole().readPassword();
    }

    @Override
    public Optional<ConsoleExtensions> extensions() {
        return getConsole().extensions();
    }

    @Override
    public String read(String message) {
        return getConsole().read(message);
    }

    @Override
    public Password readPassword(String message) {
        return getConsole().readPassword(message);
    }

    @Override
    public CachedConsole cache() throws ConsoleException {
        return getConsole().cache();
    }

    public static Console create(List<ConsoleProvider> providers, SecretService secretService) {
        List<ConsoleProvider> sorted = new ArrayList<>(providers);
        Collections.sort(sorted);
        return new DefaultConsole(sorted, secretService);
    }

    private Console getConsole() {
        return new CensoringConsole(new SPIService<>(providers, "console").get(), secretService);
    }

}
