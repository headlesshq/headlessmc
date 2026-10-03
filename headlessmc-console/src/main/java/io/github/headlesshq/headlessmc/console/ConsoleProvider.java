package io.github.headlesshq.headlessmc.console;

import io.github.headlesshq.headlessmc.console.spi.ConsoleSPI;

public interface ConsoleProvider extends ConsoleSPI<Console, ConsoleProvider> {
    int SORT_CACHE = 50;
    int SORT_JLINE = 100;
    int SORT_DEFAULT = 200;

    Console get() throws ConsoleException;

}
