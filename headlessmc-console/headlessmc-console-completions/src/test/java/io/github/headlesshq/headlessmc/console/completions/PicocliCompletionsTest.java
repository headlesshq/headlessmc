package io.github.headlesshq.headlessmc.console.completions;

import io.github.headlesshq.headlessmc.console.Completions;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PicocliCompletionsTest {
    @CommandLine.Command(name = "root", subcommands = {Sub.class})
    static class Root {
    }

    @SuppressWarnings("unused")
    @CommandLine.Command(name = "sub", aliases = {"s"}, description = "a subcommand")
    static class Sub {
        @CommandLine.Option(names = {"-f", "--force"}, description = "forces things")
        boolean force;
    }

    private record LineImpl(List<String> words, int wordIndex) implements Completions.Line {
        @Override
        public String word() {
            return words.get(wordIndex);
        }

        @Override
        public int wordCursor() {
            return word().length();
        }

        @Override
        public String line() {
            return String.join(" ", words);
        }

        @Override
        public int cursor() {
            return line().length();
        }
    }

    private List<String> complete(List<String> words, int wordIndex) {
        CommandLine commandLine = new CommandLine(Root.class);
        return complete(() -> commandLine, words, wordIndex);
    }

    private List<String> complete(Supplier<CommandLine> commandLine, List<String> words, int wordIndex) {
        return new PicocliCompletions(commandLine).stream(new LineImpl(words, wordIndex))
            .map(Completions.Candidate::getName)
            .toList();
    }

    @Test
    void completesSubcommands() {
        List<String> completions = complete(List.of(""), 0);

        assertTrue(completions.contains("sub"));
        assertTrue(completions.contains("s"));
    }

    @Test
    void completesOptions() {
        List<String> completions = complete(List.of("sub", "-"), 1);

        assertTrue(completions.stream().anyMatch(completion -> completion.contains("-force")),
                   () -> "was: " + completions);
    }

    @Test
    void completesWithAFreshCommandLinePerCall() {
        // HeadlessMcCommand supplies a new CommandLine per call, the parser configuration necessary
        // for completing an incomplete line has to be applied to that instance.
        Supplier<CommandLine> commandLine = () -> new CommandLine(Root.class);

        assertTrue(complete(commandLine, List.of(""), 0).contains("sub"));
        assertTrue(complete(commandLine, List.of("s"), 0).contains("sub"), () -> "was: " + complete(commandLine, List.of("s"), 0));
        assertTrue(complete(commandLine, List.of("sub", "-"), 1).stream().anyMatch(c -> c.contains("-force")));
        assertTrue(complete(commandLine, List.of("unmatched"), 0).isEmpty());
    }

    @Test
    void commonOptionsAreNotSuggestedForNonOptionWords() {
        assertFalse(complete(List.of(""), 0).contains("-h"));
    }

}
