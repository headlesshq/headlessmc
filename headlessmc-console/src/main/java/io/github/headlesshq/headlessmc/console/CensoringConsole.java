package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.output.SecretService;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * A {@link Console} that removes secrets with the {@link SecretService}
 * from everything it writes, including prompts,
 * before passing it on to the console it wraps.
 * Editors start empty instead of with a text containing secrets,
 * as masking the text would turn the mask into the edited value.
 */
@RequiredArgsConstructor
public class CensoringConsole implements Console {
    private final Console console;
    private final SecretService secretService;

    @Override
    public void write(String message) {
        console.write(secretService.censorSecrets(message));
    }

    @Override
    public void write(String message, String nextLine) {
        console.write(secretService.censorSecrets(message), nextLine);
    }

    @Override
    public String read() {
        return console.read();
    }

    @Override
    public Password readPassword() {
        return console.readPassword();
    }

    @Override
    public String read(String message) {
        return console.read(secretService.censorSecrets(message));
    }

    @Override
    public Password readPassword(String message) {
        return console.readPassword(secretService.censorSecrets(message));
    }

    @Override
    public Optional<ConsoleExtensions> extensions() {
        return console.extensions().map(extensions -> new ConsoleExtensions() {
            @Override
            public String edit(String initialString) {
                boolean containsSecrets = !secretService.censorSecrets(initialString).equals(initialString);
                return extensions.edit(containsSecrets ? "" : initialString);
            }

            @Override
            public String read(String prompt, Completions completions) {
                return extensions.read(secretService.censorSecrets(prompt), completions);
            }

            @Override
            public int getWidth() {
                return extensions.getWidth();
            }
        });
    }

    @Override
    public CachedConsole cache() throws ConsoleException {
        CachedConsole cached = console.cache();
        Console censored = new CensoringConsole(cached.get(), secretService);
        return new CachedConsole() {
            @Override
            public Console get() {
                return censored;
            }

            @Override
            public void close() throws ConsoleException {
                cached.close();
            }
        };
    }

}
