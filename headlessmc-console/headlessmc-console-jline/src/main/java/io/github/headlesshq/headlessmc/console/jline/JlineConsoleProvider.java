package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.config.Holder;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleException;
import io.github.headlesshq.headlessmc.console.ConsoleProvider;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Always provides the same {@link JlineConsole}, which owns the terminal.
 * Jline only allows one system terminal at a time,
 * so all users of the console have to share its terminal state.
 */
@RequiredArgsConstructor
public class JlineConsoleProvider implements ConsoleProvider {
    private final Holder<JlineConfig> config;
    private volatile @Nullable JlineConsole console;

    @Override
    public Console get() {
        if (!config.get().enabled()) {
            throw new ConsoleException("Jline console not enabled");
        }

        JlineConsole result = console;
        if (result == null) {
            synchronized (this) {
                result = console;
                if (result == null) {
                    result = new JlineConsole(config);
                    result.test();
                    console = result;
                }
            }
        }

        return result;
    }

    @Override
    public int sort() {
        return SORT_JLINE;
    }

}
