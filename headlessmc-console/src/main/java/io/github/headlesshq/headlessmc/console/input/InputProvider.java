package io.github.headlesshq.headlessmc.console.input;

import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.spi.ConsoleSPI;

public interface InputProvider extends ConsoleSPI<Input, InputProvider> {
    Input get() throws ConsoleException;

}
