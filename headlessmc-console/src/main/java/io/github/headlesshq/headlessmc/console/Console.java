package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.input.Input;
import io.github.headlesshq.headlessmc.console.output.Output;

import java.util.Optional;

public interface Console extends Input, Output {
    int SORT_JLINE = 100;
    int SORT_CONSOLE = 200;
    int SORT_STD = 300;
    int SORT_FILE_DESCRIPTOR = 400;

    void write(String message);

    void write(String message, String nextLine);

    String read();

    Password readPassword();

    default String read(String message) {
        write(message, "");
        return read();
    }

    default Password readPassword(String message) {
        write(message, "");
        return readPassword();
    }

    default Optional<ConsoleExtensions> extensions() {
        return Optional.empty();
    }

    // should not be called directly, rather use ConsoleCachingService
    default CachedConsole cache() throws ConsoleException {
        return new CachedConsole() {
            @Override
            public Console get() {
                return Console.this;
            }

            @Override
            public void close() {

            }
        };
    }

}
