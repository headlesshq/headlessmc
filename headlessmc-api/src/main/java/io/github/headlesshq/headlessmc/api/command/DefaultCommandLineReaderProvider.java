package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.logging.StdIO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.Getter;

@Getter
@ApplicationScoped
public class DefaultCommandLineReaderProvider {
    private final Instance<CommandLineReader> readers;
    private final StdIO stdIO;

    @Inject
    public DefaultCommandLineReaderProvider(@CommandLineReader.Implementation Instance<CommandLineReader> readers,
                                            StdIO stdIO) {
        this.readers = readers;
        this.stdIO = stdIO;
    }

    @Produces
    @ApplicationScoped
    public CommandLineReader get() {
        if (stdIO.getConsole().get() != null) {
            return readers.select(ConsoleCommandReader.class).get();
        } else { // e.g. in IntelliJ's run tab terminal
            return readers.select(BufferedCommandLineReader.class).get();
        }
    }

}
