package io.github.headlesshq.headlessmc.console.jline;

import io.github.headlesshq.headlessmc.console.Completions;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.utils.AttributedString;

import java.util.List;

final class CompleterImpl extends ArgumentCompleter implements Completer {
    private final Completions completions;

    CompleterImpl(Completions completions) {
        super(NullCompleter.INSTANCE);
        this.completions = completions;
    }

    @Override
    public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
        Completions.Line line = new ParsedLineAdapter(commandLine);
        completions.candidates(null, line)
            .forEach(candidate -> candidates.add(new Candidate(
                AttributedString.stripAnsi(candidate.getName()),
                candidate.getName(),
                null,
                candidate.getDescription(),
                null,
                null,
                true // TODO: Picocli: is it complete? For options dont do this?;
            )));
    }

}
