package io.github.headlesshq.headlessmc.api.command.picocli;

import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.logging.StdIO;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import picocli.CommandLine;

import java.io.PrintWriter;

public class CommandLineProvider implements Provider<CommandLine> {
    private final StdIO stdIO;
    private final Injector injector;
    private final Object command;

    @Inject
    public CommandLineProvider(StdIO stdIO, Injector injector, @RootCommand Object command) {
        this.stdIO = stdIO;
        this.injector = injector;
        this.command = command;
    }

    @Override
    public CommandLine get() {
        return new CommandLine(command, new CommandFactory(injector)) {
            @Override
            public PrintWriter getOut() {
                return stdIO.getOut().get().getWriter();
            }

            @Override
            public CommandLine setOut(PrintWriter out) {
                throw new UnsupportedOperationException("Use StdIO.setOut");
            }

            @Override
            public PrintWriter getErr() {
                return stdIO.getErr().get().getWriter();
            }

            @Override
            public CommandLine setErr(PrintWriter err) {
                throw new UnsupportedOperationException("Use StdIO.setErr");
            }
        };
    }

}
