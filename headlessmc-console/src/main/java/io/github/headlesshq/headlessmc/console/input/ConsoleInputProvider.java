package io.github.headlesshq.headlessmc.console.input;

import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.Password;
import io.github.headlesshq.headlessmc.exceptions.HeadlessMcIOException;

import java.io.Console;

public class ConsoleInputProvider implements InputProvider {
    /**
     * {@code -Dhmc.console.system-console=false} skips {@link System#console()} and reads System.in instead.
     * E.g. CheerpJ returns a Console that no input ever reaches.
     */
    public static final String ENABLED_PROPERTY = "hmc.console.system-console";

    @Override
    public Input get() {
        if (!Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "true"))) {
            throw new ConsoleException("System console disabled by " + ENABLED_PROPERTY);
        }

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
