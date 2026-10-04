package io.github.headlesshq.headlessmc.application;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.console.completions.BetterAutoComplete;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import lombok.Data;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class VersionCompletionsTest {
    @Inject
    Provider<CommandLine> commandLine;

    @Test
    public void testVersionCompletions() {
        List<String> args = List.of("server", "add", "fa");
        List<Completions.Candidate> candidates = new ArrayList<>();
        new BetterAutoComplete().complete(new SimpleLine(args, 2, 2), candidates, commandLine.get());

        // "fa" completes to the fabric platform, minus the prefix that was typed already
        assertEquals(
            List.of("bric"), candidates.stream().map(Completions.Candidate::getName).toList(), () -> "was: " + candidates
        );
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
