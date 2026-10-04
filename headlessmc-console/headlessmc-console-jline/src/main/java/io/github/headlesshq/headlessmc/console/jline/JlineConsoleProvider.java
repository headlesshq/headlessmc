package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.ConsoleProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JlineConsoleProvider implements ConsoleProvider {
    private final Holder<JlineConfig> config;

    @Override
    public Console get() {
        if (!config.get().enabled()) {
            throw new ConsoleException("Jline console not enabled");
        }

        JlineConsole console = new JlineConsole(config);
        console.test();
        return console;
    }

    @Override
    public int sort() {
        return SORT_JLINE;
    }

}
