package io.github.headlesshq.headlessmc.jline;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.command.CommandContext;
import io.github.headlesshq.headlessmc.api.command.Suggestion;
import lombok.RequiredArgsConstructor;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

/**
 * A JLine {@link Completer} that completes a given command with the current {@link CommandContext}.
 */
@RequiredArgsConstructor
public class CommandCompleter implements Completer {
    private final Application app;

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        for (Suggestion suggestion : app.getCommandLine().getActiveContext().getSuggestions(line.wordIndex(), line.wordCursor(), line.cursor(), line.words().toArray(new String[0]))) {
            candidates.add(new Candidate(suggestion.getValue(), suggestion.getValue(), null, suggestion.getDescription(), null, null, suggestion.isComplete()));
        }
    }

}
