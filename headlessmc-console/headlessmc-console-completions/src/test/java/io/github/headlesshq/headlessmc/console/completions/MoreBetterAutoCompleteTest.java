package io.github.headlesshq.headlessmc.console.completions;

import io.github.headlesshq.headlessmc.console.Completions;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// More test cases
public class MoreBetterAutoCompleteTest {
    private record Completion(int ret, List<Completions.Candidate> candidates) {
        private static final int CURSOR = 42;

        List<String> names() {
            return candidates.stream().map(Completions.Candidate::getName).collect(Collectors.toList());
        }

        String descriptionOf(String name) {
            //noinspection DataFlowIssue
            return candidates.stream()
                .filter(c -> c.getName().equals(name))
                .map(Completions.Candidate::getDescription)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no candidate named '" + name + "' in " + names()));
        }
    }

    /** Completes the given word at {@code positionInArg} inside {@code args[argIndex]}. */
    private static Completion complete(String[] args, int argIndex, int positionInArg) {
        CommandLine line = new CommandLine(Root.class);
        List<Completions.Candidate> candidates = new ArrayList<>();
        int ret = new BetterAutoComplete().complete(
            new SimpleLine(Arrays.asList(args), argIndex, positionInArg), candidates, line
        );

        return new Completion(ret, candidates);
    }

    /** Completes the last word, cursor at its end - the common "typing at the caret" case. */
    private static Completion completeAtEnd(String... args) {
        return complete(args, args.length - 1, args[args.length - 1].length());
    }

    /**
     * A {@link io.github.headlesshq.headlessmc.console.Completions.Line} that is not backed by an actual console line,
     * its words are treated as if they had been separated by single spaces.
     */
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

    @Nested
    class Subcommands {
        @Test
        public void listsAllSubcommandsAndAliasesForAnEmptyWord() {
            List<String> names = completeAtEnd("").names();
            // "t" is an alias of "test".
            assertTrue(names.containsAll(List.of("test", "t", "other")), () -> "was: " + names);
        }

        @Test
        public void filtersAndTrimsSubcommandsByPrefix() {
            List<String> names = completeAtEnd("o").names();
            assertEquals(List.of("ther"), names);
        }

        @Test
        public void trimsThePrefixOfAPartiallyTypedSubcommand() {
            assertEquals(List.of("st"), completeAtEnd("te").names());
            assertEquals(List.of("t"), completeAtEnd("tes").names());
        }

        @Test
        public void carriesTheSubcommandDescription() {
            Completion completion = completeAtEnd("");
            assertEquals("other command", completion.descriptionOf("other"));
        }

        //@Test
        //public void returnsCursorWhenThereAreCandidatesAndMinusOneOtherwise() {
        //    assertEquals(Completion.CURSOR, completeAtEnd("").ret());
        //    assertEquals(-1, completeAtEnd("does-not-exist").ret());
        //}
    }

    @Nested
    class Positionals {
        @Test
        public void suggestsTheFirstPositionalBeforeItIsFilled() {
            List<String> names = completeAtEnd("test", "").names();
            assertTrue(names.containsAll(List.of("can1", "can2")), () -> "was: " + names);
            assertFalse(names.contains("secondparam1"), () -> "was: " + names);
            assertFalse(names.contains("secondparam2"), () -> "was: " + names);
        }

        @Test
        public void suggestsTheSecondPositionalOnceTheFirstIsFilled() {
            List<String> names = completeAtEnd("test", "can1", "").names();
            assertTrue(names.containsAll(List.of("secondparam1", "secondparam2")), () -> "was: " + names);
            assertFalse(names.contains("can1"), () -> "was: " + names);
            assertFalse(names.contains("can2"), () -> "was: " + names);
        }

        @Test
        public void offersOptionsAlongsideThePositional() {
            List<String> names = completeAtEnd("test", "").names();
            assertTrue(names.containsAll(List.of("--test", "--color", "--flag")), () -> "was: " + names);
        }

        @Test
        public void filtersAndTrimsThePositionalByPrefix() {
            List<String> names = completeAtEnd("test", "can").names();
            // "can1"/"can2" trimmed to their suffixes; options are filtered out by the prefix.
            assertTrue(names.containsAll(List.of("1", "2")), () -> "was: " + names);
            assertFalse(names.contains("--test"), () -> "was: " + names);
        }

        @Test
        public void yieldsNothingForANonMatchingPositionalPrefix() {
            Completion completion = completeAtEnd("test", "zzz");
            assertTrue(completion.names().isEmpty(), () -> "was: " + completion.names());
            assertEquals(-1, completion.ret());
        }

        @Test
        public void resolvesAliasesTheSameWayAsTheCommand() {
            assertTrue(completeAtEnd("t", "").names().containsAll(List.of("can1", "can2")));
            assertTrue(completeAtEnd("t", "can1", "").names().containsAll(List.of("secondparam1", "secondparam2")));
        }
    }

    @Nested
    class Options {
        @Test
        public void listsAllOptionNamesAfterASingleDash() {
            List<String> names = completeAtEnd("test", "-").names();
            // one leading dash already committed, so the trimmed remainder keeps the second dash.
            assertTrue(names.containsAll(List.of("-test", "-color", "-flag")), () -> "was: " + names);
        }

        @Test
        public void listsOptionNamesAfterADoubleDash() {
            List<String> names = completeAtEnd("test", "--").names();
            assertTrue(names.containsAll(List.of("test", "color", "flag")), () -> "was: " + names);
        }

        @Test
        public void filtersAndTrimsOptionsByPrefix() {
            assertEquals(List.of("or"), completeAtEnd("test", "--col").names());
        }

        @Test
        public void carriesTheOptionDescription() {
            Completion completion = completeAtEnd("test", "--");
            assertEquals("test option", completion.descriptionOf("test"));
            assertEquals("a flag", completion.descriptionOf("flag"));
        }

        @Test
        public void keepsCompletingPositionalsAfterAConsumedFlag() {
            // A zero-arity flag consumes no value, so the first positional is still expected next.
            List<String> names = completeAtEnd("test", "--flag", "").names();
            assertTrue(names.containsAll(List.of("can1", "can2")), () -> "was: " + names);
            assertFalse(names.contains("secondparam1"), () -> "was: " + names);
        }
    }

    @Nested
    class OptionValues {
        @Test
        public void completesAnOptionValueAfterASpace() {
            List<String> names = completeAtEnd("test", "--color", "").names();
            assertTrue(names.containsAll(List.of("red", "green", "blue")), () -> "was: " + names);
            // the option's own candidates only - no positionals, no other options.
            assertFalse(names.contains("can1"), () -> "was: " + names);
            assertFalse(names.contains("--test"), () -> "was: " + names);
        }

        @Test
        public void completesAnOptionValueAfterTheSeparator() {
            List<String> names = completeAtEnd("test", "--color=").names();
            assertTrue(names.containsAll(List.of("red", "green", "blue")), () -> "was: " + names);
        }

        @Test
        public void filtersAndTrimsAnOptionValueByPrefix() {
            assertEquals(List.of("ed"), completeAtEnd("test", "--color=r").names());
        }

        @Test
        public void yieldsNothingForANonMatchingOptionValue() {
            Completion completion = completeAtEnd("test", "--color=x");
            assertTrue(completion.names().isEmpty(), () -> "was: " + completion.names());
            assertEquals(-1, completion.ret());
        }
    }

    @Nested
    class ExhaustedPositionals {
        @Test
        public void doesNotSuggestPositionalsOnceEveryPositionalSlotIsFilled() {
            List<String> names = completeAtEnd("test", "can1", "secondparam1", "").names();
            // both positionals are provided, so none of their candidates may reappear.
            assertFalse(names.contains("can1"), () -> "was: " + names);
            assertFalse(names.contains("can2"), () -> "was: " + names);
            assertFalse(names.contains("secondparam1"), () -> "was: " + names);
            assertFalse(names.contains("secondparam2"), () -> "was: " + names);
        }

        @Test
        public void stillOffersOptionsWhenPositionalsAreExhausted() {
            // options remain valid completions even after all positionals are filled.
            List<String> names = completeAtEnd("test", "can1", "secondparam1", "").names();
            assertTrue(names.containsAll(List.of("--test", "--color", "--flag")), () -> "was: " + names);
        }

        @Test
        public void yieldsNothingWhenThereIsNothingLeftToComplete() {
            // "plain" has a single positional and no options/subcommands - once it is filled there
            // is genuinely nothing left to suggest.
            Completion completion = completeAtEnd("plain", "only1", "");
            assertTrue(completion.names().isEmpty(), () -> "was: " + completion.names());
            assertEquals(-1, completion.ret());
        }

        @Test
        public void stillCompletesThePositionalBeforeItIsFilled() {
            List<String> names = completeAtEnd("plain", "").names();
            assertTrue(names.containsAll(List.of("only1", "only2")), () -> "was: " + names);
        }
    }

    @Nested
    class SmartCompletions {
        @Test
        public void passesTheLineToCompletions() {
            List<String> names = completeAtEnd("smart", "").names();
            // the Completions get the whole line and answer based on it.
            assertEquals(List.of("smart:1:0"), names);
        }

        @Test
        public void filtersAndTrimsTheCandidatesOfCompletions() {
            // "smart:1:5" is the only candidate for this line, "smart" of it is already typed.
            assertEquals(List.of(":1:5"), completeAtEnd("smart", "smart").names());
        }

        @Test
        public void carriesTheDescriptionOfCompletions() {
            assertEquals("smart candidate", completeAtEnd("smart", "").descriptionOf("smart:1:0"));
        }
    }

    @Nested
    class ParserConfiguration {
        @Test
        public void doesNotThrowOnAWordThatDoesNotParse() {
            // the incomplete word has to be collected as a parse error instead of being thrown as an
            // UnmatchedArgumentException - that requires configuring the CommandLine we parse with.
            Completion completion = completeAtEnd("does-not-exist");
            assertTrue(completion.names().isEmpty(), () -> "was: " + completion.names());
            assertEquals(-1, completion.ret());
        }

        @Test
        public void doesNotLeaveErrorCollectionEnabledOnTheParsedCommandLine() {
            CommandLine commandLine = new CommandLine(Root.class);
            assertFalse(commandLine.getCommandSpec().parser().collectErrors());
            new BetterAutoComplete().complete(new SimpleLine(List.of("o"), 0, 1), new ArrayList<>(), commandLine);

            assertFalse(commandLine.getCommandSpec().parser().collectErrors());
        }
    }

    @CommandLine.Command(name = "root", subcommands = {Test1.class, Other.class, Plain.class, Smart.class})
    static class Root {
    }

    @SuppressWarnings({"NotNullFieldNotInitialized", "unused"})
    @CommandLine.Command(name = "test", aliases = {"t"})
    static class Test1 {
        @CommandLine.Option(names = "--test", description = "test option")
        String option;

        @CommandLine.Option(names = "--color", description = "color option", completionCandidates = Colors.class)
        String color;

        @CommandLine.Option(names = "--flag", description = "a flag")
        boolean flag;

        @CommandLine.Parameters(index = "0", description = "first param", completionCandidates = FirstParams.class)
        String firstParam;

        @CommandLine.Parameters(index = "1", arity = "0..1", description = "second param",
            completionCandidates = SecondParams.class)
        String secondParam;
    }

    @CommandLine.Command(name = "other", description = "other command")
    static class Other {
    }

    @SuppressWarnings({"NotNullFieldNotInitialized", "unused"})
    @CommandLine.Command(name = "plain", description = "no options, one positional")
    static class Plain {
        @CommandLine.Parameters(index = "0", description = "only param", completionCandidates = OnlyParams.class)
        String onlyParam;
    }

    @SuppressWarnings({"NotNullFieldNotInitialized", "unused"})
    @CommandLine.Command(name = "smart", description = "completes via Completions")
    static class Smart {
        @CommandLine.Parameters(index = "0", description = "smart param", completionCandidates = SmartCandidates.class)
        String smartParam;
    }

    /** Answers with a single candidate that describes the {@link Completions.Line} it was given. */
    static class SmartCandidates implements Completions {
        @Override
        public Iterable<Candidate> candidates(@Nullable Object spec, Line line) {
            return List.of(new Candidate(
                line.words().getFirst() + ":" + line.wordIndex() + ":" + line.wordCursor(), "smart candidate"
            ));
        }

        @Override
        public Iterator<String> iterator() {
            throw new AssertionError("Completions must be completed via candidates(Line)");
        }
    }

    static class Colors implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return List.of("red", "green", "blue").iterator();
        }
    }

    static class FirstParams implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return List.of("can1", "can2").iterator();
        }
    }

    static class SecondParams implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return List.of("secondparam1", "secondparam2").iterator();
        }
    }

    static class OnlyParams implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return List.of("only1", "only2").iterator();
        }
    }

}
