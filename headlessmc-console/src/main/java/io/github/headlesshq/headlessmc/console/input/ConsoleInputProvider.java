package io.github.headlesshq.headlessmc.console.input;

import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.Password;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;

import java.io.Console;

public class ConsoleInputProvider implements InputProvider {
    @Override
    public Input get() {
        Console console = System.console();
        if (console == null) {
            throw new ConsoleException("Console unavailable");
        }

        return new Input() {
            @Override
            public String read() {
                String line = console.readLine();
                if (line == null) {
                    throw new HeadlessMcIOException("Console has been closed");
                }

                return line;
            }

            @Override
            public Password readPassword() {
                char[] password = console.readPassword();
                if (password == null) {
                    throw new HeadlessMcIOException("Console has been closed");
                }

                return new Password(password);
            }
        };
    }

    @Override
    public int sort() {
        return io.github.headlesshq.headlessmc.console.Console.SORT_CONSOLE;
    }

}
