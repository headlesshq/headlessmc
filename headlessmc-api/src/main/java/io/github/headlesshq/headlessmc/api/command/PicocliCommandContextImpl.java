package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.command.picocli.CommandLineParser;
import io.github.headlesshq.headlessmc.api.command.picocli.PicocliFactory;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Dependent
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class PicocliCommandContextImpl implements PicocliCommandContext {
    private final List<CommandLine> history = new ArrayList<>();
    private final PicocliFactory commandLineFactory;
    private volatile int exitCode = 0;

    @Override
    public void execute(String command) throws CommandException {
        CommandLineParser parser = new CommandLineParser();
        String[] args = parser.parse(command);
        executeArgs(args);
    }

    @Override
    public void executeArgs(String... args) throws CommandException {
        try {
            CommandLine commandLine = commandLineFactory.create();
            getHistory().add(commandLine);
            exitCode = commandLine.execute(args);
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandException(e);
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
