package io.github.headlesshq.headlessmc.console.spi;

import io.github.headlesshq.headlessmc.console.ConsoleException;

public interface ConsoleSPI<T, SELF extends ConsoleSPI<T, SELF>> extends Comparable<SELF> {
    T get() throws ConsoleException;

    int sort();

    @Override
    default int compareTo(SELF o) {
        return Integer.compare(sort(), o.sort());
    }

}
