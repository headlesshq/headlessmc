package io.github.headlesshq.headlessmc.console.completions;

import io.github.headlesshq.headlessmc.console.Completions;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class PicocliCompletions implements Completions {
    private static final Set<String> COMMON_COMPLETIONS = new HashSet<>(Arrays.asList(
        "-h",
        "--help",
        "-v",
        "--version"
    ));

    private final Supplier<CommandLine> commandLine;

    public Stream<Candidate> stream(Line line) {
        List<Candidate> autoCompleted = new ArrayList<>();
        new BetterAutoComplete().complete(line, autoCompleted, this.commandLine.get());

        List<Candidate> candidates = new ArrayList<>();
        autoCompleted.forEach(candidate -> {
            String completion = line.word() + candidate.getName();
            // prevent --help --version -h and -v from shopping up everywhere
            if (!line.word().startsWith("-")
                && COMMON_COMPLETIONS.contains(completion.toLowerCase(Locale.ENGLISH))) {
                return;
            }

            candidates.add(new Candidate(completion, candidate.getDescription()));
        });

        return candidates.stream();
    }

    @Override
    public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
        // if (spec != null) warn!
        return stream(line).collect(Collectors.toList());
    }

    @Override
    public Iterator<String> iterator() {
        return stream(new Line() {
                @Override
                public String word() {
                    return "";
                }

                @Override
                public int wordCursor() {
                    return 0;
                }

                @Override
                public int wordIndex() {
                    return 0;
                }

                @Override
                public List<String> words() {
                    return new ArrayList<>(0);
                }

                @Override
                public String line() {
                    return "";
                }

                @Override
                public int cursor() {
                    return 0;
                }
            }
        ).map(Candidate::getName)
            .iterator();
    }

}
