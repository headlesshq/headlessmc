package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.logging.StdIO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class DefaultCommandLineReaderProvider implements Provider<CommandLineReader> {
    private final StdIO stdIO;

    @Override
    public CommandLineReader get() {
        if (stdIO.getConsole().get() != null) {
            return new ConsoleCommandReader();
        } else { // e.g. in IntelliJ's run tab terminal
            return new BufferedCommandLineReader();
        }
    }

}
