package io.github.headlesshq.headlessmc.console.completions;

import io.github.headlesshq.headlessmc.console.Completions;
import lombok.Data;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
public class BetterAutoCompleteTest {
    @Test
    public void testAutoComplete() {
        CommandLine commandLine = new CommandLine(Smart.class);
        List<Completions.Candidate> candidates = new ArrayList<>();
        SimpleLine line = new SimpleLine(Arrays.asList("fa"), 0, 2);
        new BetterAutoComplete().complete(line, candidates, commandLine);

        // "fa" narrows the candidates down to fabric, minus the prefix that was typed already
        assertEquals(
            List.of("bric"), candidates.stream().map(Completions.Candidate::getName).toList(), () -> "was: " + candidates
        );
    }

    @SuppressWarnings({"unused"})
    @CommandLine.Command(name = "smart", description = "completes via Completions")
    static class Smart {
        @CommandLine.Parameters(index = "0", description = "smart param", completionCandidates = PlatformCompletions.class)
        String smartParam;
    }

    static class PlatformCompletions implements Completions {
        @Override
        public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
            return Arrays.asList(
                new Candidate("fabric", null),
                new Candidate("forge", null),
                new Candidate("vanilla", null)
            );
        }

        @Override
        public Iterator<String> iterator() {
            throw new AssertionError("Completions must be completed via candidates(Line)");
        }
    }

    @Data
    static final class SimpleLine implements Completions.Line {
        private final List<String> words;
        private final int wordIndex;
        private final int wordCursor;

        @Override
        public List<String> words() {
            return words;
        }

        @Override
        public int wordIndex() {
            return wordIndex;
        }

        @Override
        public int wordCursor() {
            return wordCursor;
        }

        @Override
        public String word() {
            return wordIndex >= 0 && wordIndex < words.size() ? words.get(wordIndex) : "";
        }

        @Override
        public String line() {
            return String.join(" ", words);
        }

        @Override
        public int cursor() {
            int cursor = 0;
            for (int i = 0; i < wordIndex && i < words.size(); i++) {
                cursor += words.get(i).length() + 1;
            }

            return cursor + wordCursor;
        }
    }

}
