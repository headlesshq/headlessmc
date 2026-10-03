package io.github.headlesshq.headlessmc.console.cache;

import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;

/**
 * It's easier to open a new Jline Terminal everytime
 * the JlineConsole {@link Console} object is used.
 * Because otherwise we would have to close the Terminal
 * whenever we spawn a subprocess that may spawn a
 * Terminal itself.
 * Some commands however can be safely run with a
 * cached Jline Terminal as they will not spawn
 * a process.
 */
public interface CachedConsole extends AutoCloseable {
    Console get();

    void close() throws ConsoleException;

    /**
     * This is a marker interface for such commands.
     */
    interface Enabled {
    }

}
