package io.github.headlesshq.headlessmc.api.command.picoli;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.Command;
import io.github.headlesshq.headlessmc.api.process.InAndOutProvider;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;

import java.io.PrintWriter;

@RequiredArgsConstructor
public class CommandLineFactory<T extends HeadlessMc> {
    private final T context;
    private final Object command;

    public CommandLine create() {
        return new CommandLine(command, new Factory<>(context)) {
            @Override
            public PrintWriter getOut() {
                return context.getCommandLine().getInAndOutProvider().getOut().get().getWriter();
            }

            @Override
            public CommandLine setOut(PrintWriter out) {
                throw new UnsupportedOperationException("Use " + InAndOutProvider.class.getName() + ".setOut");
            }

            @Override
            public PrintWriter getErr() {
                return context.getCommandLine().getInAndOutProvider().getErr().get().getWriter();
            }

            @Override
            public CommandLine setErr(PrintWriter err) {
                throw new UnsupportedOperationException("Use " + InAndOutProvider.class.getName() + ".setErr");
            }
        };
    }

    @RequiredArgsConstructor
    private static final class Factory<T> implements CommandLine.IFactory {
        private final T context;

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public <K> K create(Class<K> cls) throws Exception {
            K result = CommandLine.defaultFactory().create(cls);
            if (result instanceof Command) {
                if (!((Command<?>) result).getContextType().isInstance(context)) {
                    throw new IllegalArgumentException("Cannot inject context " + context + " into " + cls);
                }

                ((Command) result).setContext(context);
            }

            return result;
        }
    }

}
