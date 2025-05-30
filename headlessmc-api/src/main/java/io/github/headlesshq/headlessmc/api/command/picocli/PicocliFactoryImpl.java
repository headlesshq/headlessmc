package io.github.headlesshq.headlessmc.api.command.picocli;

import io.github.headlesshq.headlessmc.api.command.PicocliCommandProvider;
import io.github.headlesshq.headlessmc.api.logging.StdIO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

import java.io.PrintWriter;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PicocliFactoryImpl implements PicocliFactory {
    private final Event<CommandLineEvent> event;
    private final CommandFactory commandFactory;
    private final PicocliCommandProvider command;
    private final StdIO stdIO;

    @Override
    @Produces
    @Dependent
    public CommandLine create() {
        CommandLine commandLine = configureCommandLine(createCommandLine());
        CommandLineEvent event = new CommandLineEvent(commandLine);
        this.event.fire(event);
        return event.getCommandLine();
    }

    private CommandLine createCommandLine() {
        return new CommandLine(command.getPicocliCommand(), commandFactory) {
            @Override
            public PrintWriter getOut() {
                return stdIO.getOut().get().getWriter();
            }

            @Override
            public CommandLine setOut(PrintWriter out) {
                // throw new UnsupportedOperationException("Use stdIO.setOut");
                return this;
            }

            @Override
            public PrintWriter getErr() {
                // TODO: actually here I would like to use a logger instead
                return stdIO.getErr().get().getWriter();
            }

            @Override
            public CommandLine setErr(PrintWriter err) {
                // throw new UnsupportedOperationException("Use stdIO.setErr");
                return this;
            }
        };
    }

    private CommandLine configureCommandLine(CommandLine commandLine) {
        commandLine.setExitCodeExceptionMapper(t -> {
            throw new CommandError(t);
        });

        commandLine.setExecutionExceptionHandler((ex, cl, pr) -> {
            throw new CommandError(ex);
        });

        commandLine.setParameterExceptionHandler((ex, args) -> {
            throw new CommandError(ex);
        });

        return commandLine;
    }

}
