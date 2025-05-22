package io.github.headlesshq.headlessmc.api.command.picocli;

import io.github.headlesshq.headlessmc.api.di.Injector;
import io.github.headlesshq.headlessmc.api.logging.StdIO;
import picocli.CommandLine;

import java.io.PrintWriter;

public class CommandLineFactory {
    public CommandLine create(StdIO io, Injector injector, Object command) {
        return new CommandLine(command, new CommandFactory(injector)) {
            @Override
            public PrintWriter getOut() {
                return io.getOut().get().getWriter();
            }

            @Override
            public CommandLine setOut(PrintWriter out) {
                throw new UnsupportedOperationException("Use StdIO.setOut");
            }

            @Override
            public PrintWriter getErr() {
                return io.getErr().get().getWriter();
            }

            @Override
            public CommandLine setErr(PrintWriter err) {
                throw new UnsupportedOperationException("Use StdIO.setErr");
            }
        };
    }

}
