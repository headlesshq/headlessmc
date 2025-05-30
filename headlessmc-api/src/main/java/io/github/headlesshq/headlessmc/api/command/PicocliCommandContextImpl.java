package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.command.picocli.CommandError;
import io.github.headlesshq.headlessmc.api.command.picocli.CommandLineParser;
import io.github.headlesshq.headlessmc.api.command.picocli.PicocliFactory;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Dependent
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class PicocliCommandContextImpl implements PicocliCommandContext {
    private final Deque<PastExecution> history = new ArrayDeque<>();
    private final PicocliFactory commandLineFactory;

    @Override
    public void execute(String command) throws CommandException {
        CommandLineParser parser = new CommandLineParser();
        String[] args = parser.parse(command);
        executeArgs(args);
    }

    @Override
    @SneakyThrows
    public void executeArgs(String... args) throws CommandException {
        CommandLine commandLine = null;
        Integer exitCode = null;
        try {
            commandLine = commandLineFactory.create();
            exitCode = commandLine.execute(args);
            // TODO: Picocli likes to swallow exceptions that happen during initialization, e.g. in the Injector
        } catch (CommandError e) {
            exitCode = -1;
            // TODO: only use CommandException to communicate!
            // TODO: explode on other exception
            if (e.getCause() instanceof CommandException) {
                throw (CommandException) e.getCause();
            }

            throw new CommandException(e.getCause());
        } finally {
            if (commandLine != null) {
                getHistory().addFirst(new PastExecution(commandLine, exitCode == null ? -1 : exitCode));
            }
        }
    }

    @Override
    public List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) {
        List<CharSequence> suggestions = new ArrayList<>();
        AutoComplete.complete(commandLineFactory.create().getCommandSpec(), args, argIndex, positionInArg, cursor, suggestions);
        return suggestions.stream()
                .map(s -> new Suggestion(s.toString(), null, true))
                .collect(Collectors.toList());
    }

}
