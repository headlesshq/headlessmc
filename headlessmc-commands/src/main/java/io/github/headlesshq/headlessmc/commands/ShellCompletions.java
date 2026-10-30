package io.github.headlesshq.headlessmc.commands;

import io.github.headlesshq.headlessmc.console.Completions;
import io.github.headlesshq.headlessmc.console.completions.PicocliCompletions;
import jakarta.enterprise.context.Dependent;
import picocli.CommandLine;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides completions for shells like zsh, by running {@code headlessmc __complete <words...> <current word>}.
 * <p>
 * The last word is the word being completed, it is empty if the cursor is after a space.
 * Prints one candidate per line as {@code <name>\t<description>}, followed by a last line
 * {@code :<directive>}, where the directive is a bitmask of {@link #DIRECTIVE_ERROR},
 * {@link #DIRECTIVE_NO_SPACE} and {@link #DIRECTIVE_NO_FILE}.
 * This is the same format as the {@code __complete} command of
 * <a href="https://github.com/spf13/cobra/blob/main/site/content/completions/_index.md">Cobra</a>.
 * <p>
 * This is handled before picocli parses the arguments, so that the words reach the completer unchanged,
 * picocli would e.g. consume {@code --} or expand {@code @files}.
 */
@Dependent
public class ShellCompletions {
    public static final String COMMAND = "__complete";
    public static final int DIRECTIVE_ERROR = 1;
    public static final int DIRECTIVE_NO_SPACE = 2;
    public static final int DIRECTIVE_NO_FILE = 4;

    private static final Pattern MARKUP = Pattern.compile("@\\|\\S+\\s|\\|@");
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\t\\r\\n]");
    private static final Pattern WHITESPACE = Pattern.compile("(%n|\\s)+");

    /**
     * Prints the completions for the given words.
     *
     * @param commandLine the command line to complete.
     * @param words       the words of the line, the last one is the word being completed.
     * @param out         the stream to print the completions to.
     * @return the exit code.
     */
    public int complete(CommandLine commandLine, List<String> words, PrintStream out) {
        List<String> line = words.isEmpty() ? List.of("") : words;
        List<Completions.Candidate> candidates;
        try {
            candidates = new PicocliCompletions(() -> commandLine)
                .stream(new Line(line, line.size() - 1))
                .distinct()
                .toList();
        } catch (RuntimeException e) {
            out.println(":" + DIRECTIVE_ERROR);
            return 1;
        }

        int directive = 0;
        if (!candidates.isEmpty() || !expectsFile(commandLine, line.subList(0, line.size() - 1))) {
            directive |= DIRECTIVE_NO_FILE;
        }

        if (!candidates.isEmpty() && candidates.stream().allMatch(candidate -> candidate.getName().endsWith("="))) {
            directive |= DIRECTIVE_NO_SPACE;
        }

        StringBuilder builder = new StringBuilder();
        for (Completions.Candidate candidate : candidates) {
            builder.append(LINE_BREAKS.matcher(candidate.getName()).replaceAll(" "))
                .append('\t')
                .append(candidate.getDescription() == null ? "" : sanitize(candidate.getDescription()))
                .append('\n');
        }

        builder.append(':').append(directive).append('\n');
        out.print(builder);
        out.flush();
        return 0;
    }

    /**
     * Determines if the argument following the given words is a {@link Path} or {@link File}.
     * Only walks the words through the subcommands, options and positionals,
     * picocli does not provide the parse result of a subcommand that is missing a required parameter.
     *
     * @param commandLine the command line to parse the words with.
     * @param previous    the words before the word being completed.
     * @return {@code true} if the shell should complete files.
     */
    boolean expectsFile(CommandLine commandLine, List<String> previous) {
        CommandLine.Model.CommandSpec spec = commandLine.getCommandSpec();
        CommandLine.Model.OptionSpec pending = null;
        int positionals = 0;
        for (String word : previous) {
            if (pending != null) {
                pending = null;
                continue;
            }

            CommandLine subcommand = positionals == 0 ? spec.subcommands().get(word) : null;
            if (subcommand != null) {
                spec = subcommand.getCommandSpec();
            } else if (word.startsWith("-") && !word.equals("-")) {
                CommandLine.Model.OptionSpec option = spec.findOption(word);
                if (option != null && option.arity().min() > 0) {
                    pending = option;
                }
            } else {
                positionals++;
            }
        }

        if (pending != null) {
            return isFile(pending.type());
        }

        for (CommandLine.Model.PositionalParamSpec positional : spec.positionalParameters()) {
            if (positional.index().min() <= positionals && positionals <= positional.index().max()) {
                return isFile(positional.type()) || isFile(positional.auxiliaryTypes()[0]);
            }
        }

        return false;
    }

    private boolean isFile(Class<?> type) {
        return Path.class.isAssignableFrom(type) || File.class.isAssignableFrom(type);
    }

    private String sanitize(String value) {
        return WHITESPACE.matcher(MARKUP.matcher(value).replaceAll("")).replaceAll(" ").trim();
    }

    private record Line(List<String> words, int wordIndex) implements Completions.Line {
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

}
