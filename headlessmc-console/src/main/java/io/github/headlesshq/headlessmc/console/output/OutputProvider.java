package io.github.headlesshq.headlessmc.console.output;

import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.spi.ConsoleSPI;

public interface OutputProvider extends ConsoleSPI<Output, OutputProvider> {
    Output get() throws ConsoleException;

}
