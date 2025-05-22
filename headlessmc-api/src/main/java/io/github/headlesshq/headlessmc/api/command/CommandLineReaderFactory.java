package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.logging.StdIO;

@FunctionalInterface
public interface CommandLineReaderFactory {
    CommandLineReader create(StdIO io);

    static CommandLineReaderFactory defaultFactory() {
        return io -> {
            if (io.getConsole().get() != null) {
                return new ConsoleCommandReader();
            } else { // e.g. in IntelliJ's run tab terminal
                return new BufferedCommandLineReader();
            }
        };
    }

}
