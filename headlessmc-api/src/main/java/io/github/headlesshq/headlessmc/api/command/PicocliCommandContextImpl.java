package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.command.picocli.CommandLineParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.AutoComplete;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ApplicationScoped
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class PicocliCommandContextImpl implements PicocliCommandContext {
    private final CommandLine picocli;
    private volatile int exitCode = 0;

    @Override
    public void execute(String command) throws CommandException {
        try {
            CommandLineParser parser = new CommandLineParser();
            String[] args = parser.parse(command);
            exitCode = picocli.execute(args);
            // TODO: make use of exit code
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }

    @Override
    public List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) {
        List<CharSequence> suggestions = new ArrayList<>();
        AutoComplete.complete(picocli.getCommandSpec(), args, argIndex, positionInArg, cursor, suggestions);
        return suggestions.stream()
                .map(s -> new Suggestion(s.toString(), null, true))
                .collect(Collectors.toList());
    }

}
