package io.github.headlesshq.headlessmc.console.completions;

import io.github.headlesshq.headlessmc.console.Completions;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.lang.reflect.Field;
import java.util.*;

/**
 * A better {@link picocli.AutoComplete}.
 * Has better handling for positional parameters.
 */
@Slf4j
@RegisterForReflection(targets = CommandLine.ParseResult.class) // access tentativeMatch
@RequiredArgsConstructor
public class BetterAutoComplete {
    @SuppressWarnings("unchecked")
    private List<Object> tentativeMatch(CommandLine.ParseResult parseResult) {
        try {
            Field field = CommandLine.ParseResult.class.getDeclaredField("tentativeMatch");
            field.setAccessible(true);
            return (List<Object>) field.get(parseResult);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.error("Failed to get tentativeMatch", e);
            return new ArrayList<>();
        }
    }

    public int complete(
        Completions.Line line,
        List<Completions.Candidate> candidates,
        CommandLine commandLine
    ) {
        String[] args = line.words().toArray(new String[0]);
        int argIndex = line.wordIndex();
        int positionInArg = line.wordCursor();
        int cursor = line.cursor();

        if (argIndex == args.length) {
            String[] copy = new String[args.length + 1];
            System.arraycopy(args, 0, copy, 0, args.length);
            args = copy;
            args[argIndex] = "";
        }

        if (argIndex < 0 || argIndex >= args.length) {
            throw new IllegalArgumentException("Invalid argIndex " + argIndex + ": args array only has " + args.length + " elements.");
        }

        if (positionInArg < 0 || positionInArg > args[argIndex].length()) {
            throw new IllegalArgumentException("Invalid positionInArg " + positionInArg + ": args[" + argIndex + "] (" + args[argIndex] + ") only has " + args[argIndex].length() + " characters.");
        }

        String currentArg = args[argIndex];
        CommandLine.Model.CommandSpec spec = commandLine.getCommandSpec();
        boolean reset = spec.parser().collectErrors();
        try {
            String committedPrefix = currentArg.substring(0, positionInArg);

            spec.parser().collectErrors(true);
            CommandLine.ParseResult parseResult = commandLine.parseArgs(args);
            List<Object> tentativeMatch = tentativeMatch(parseResult);

            if (argIndex >= tentativeMatch.size()) {
                Object startPoint = findCompletionStartPoint(parseResult);
                if (startPoint instanceof CommandLine.Model.CommandSpec) {
                    CommandLine.Model.CommandSpec cmd = (CommandLine.Model.CommandSpec) startPoint;
                    addSubcommandsAndOptions(cmd, candidates);
                    addNextPositional(cmd, parseResult, line, candidates, cmd.userObject());
                } else {
                    addCandidatesForArgsFollowing(startPoint, line, candidates, parseResult.commandSpec().userObject());
                }
            } else {
                CommandLine.Model.CommandSpec lastSpec = parseResult.commandSpec();
                for (Object match : tentativeMatch.reversed()) {
                    if (match instanceof CommandLine.Model.CommandSpec) {
                        lastSpec = (CommandLine.Model.CommandSpec) match;
                        break;
                    }
                }

                Object obj = tentativeMatch.get(argIndex);
                if (obj instanceof CommandLine.Model.CommandSpec) { // subcommand
                    addCandidatesForArgsFollowing(
                        ((CommandLine.Model.CommandSpec) obj).parent(),
                        line,
                        candidates,
                        ((CommandLine.Model.CommandSpec) obj).userObject()
                    );

                } else if (obj instanceof CommandLine.Model.OptionSpec) { // option
                    int sep = currentArg.indexOf(spec.parser().separator());
                    if (sep < 0 || positionInArg < sep) { // no '=' or cursor before '='
                        addCandidatesForArgsFollowing(
                            findCommandFor((CommandLine.Model.OptionSpec) obj, spec),
                            line,
                            candidates,
                            lastSpec.userObject()
                        );
                    } else {
                        addCandidatesForArgsFollowing((CommandLine.Model.OptionSpec) obj, line, candidates,
                                                      parseResult.commandSpec().userObject());

                        int sepLength = spec.parser().separator().length();
                        if (positionInArg < sep + sepLength) {
                            int posInSeparator = positionInArg - sep;
                            String prefix = spec.parser().separator().substring(posInSeparator);
                            for (int i = 0; i < candidates.size(); i++) {
                                candidates.set(
                                    i,
                                    new Completions.Candidate(
                                        prefix + candidates.get(i).getName(),
                                        candidates.get(i).getDescription()
                                    )
                                );
                            }

                            committedPrefix = currentArg.substring(sep, positionInArg);
                        } else {
                            committedPrefix = currentArg.substring(sep + sepLength, positionInArg);
                        }
                    }
                } else if (obj instanceof CommandLine.Model.PositionalParamSpec) { // positional
                    // only suggest candidates for the positional at the current index, plus the
                    // command's subcommands and options - not the following positionals, those are
                    // shown once the earlier ones have been filled.
                    CommandLine.Model.PositionalParamSpec positional = (CommandLine.Model.PositionalParamSpec) obj;
                    addSubcommandsAndOptions(findCommandFor(positional, spec), candidates);
                    addCandidatesForArgsFollowing(positional, line, candidates, lastSpec.userObject());
                } else {
                    int i = argIndex - 1;
                    while (i > 0 && !isPicocliModelObject(tentativeMatch.get(i))) {
                        i--;
                    }
                    if (i < 0) {
                        return -1;
                    }

                    Object match = tentativeMatch.get(i);
                    addCandidatesForArgsFollowing(match, line, candidates, lastSpec.userObject());
                }
            }

            filterAndTrimMatchingPrefix(committedPrefix, candidates);
            return candidates.isEmpty() ? -1 : cursor;
        } finally {
            spec.parser().collectErrors(reset);
        }
    }

    private @Nullable Object findCompletionStartPoint(CommandLine.ParseResult parseResult) {
        List<Object> tentativeMatches = tentativeMatch(parseResult);
        for (int i = 1; i <= tentativeMatches.size(); i++) {
            Object found = tentativeMatches.get(tentativeMatches.size() - i);
            if (found instanceof CommandLine.Model.CommandSpec) {
                return found;
            }

            if (found instanceof CommandLine.Model.ArgSpec) {
                CommandLine.Range arity = ((CommandLine.Model.ArgSpec) found).arity();
                if (i < arity.min()) {
                    return found; // not all parameters have been supplied yet
                } else {
                    return findCommandFor((CommandLine.Model.ArgSpec) found, parseResult.commandSpec());
                }
            }

        }

        return parseResult.commandSpec();
    }

    private CommandLine.Model.@Nullable CommandSpec findCommandFor(
        CommandLine.Model.ArgSpec arg,
        CommandLine.Model.CommandSpec cmd
    ) {
        return (arg instanceof CommandLine.Model.OptionSpec) ? findCommandFor(
            (CommandLine.Model.OptionSpec) arg,
            cmd
        ) : findCommandFor((CommandLine.Model.PositionalParamSpec) arg, cmd);
    }

    private CommandLine.Model.@Nullable CommandSpec findCommandFor(
        CommandLine.Model.OptionSpec option,
        CommandLine.Model.CommandSpec commandSpec
    ) {
        for (CommandLine.Model.OptionSpec defined : commandSpec.options()) {
            if (defined == option) {
                return commandSpec;
            }
        }
        for (CommandLine sub : commandSpec.subcommands().values()) {
            CommandLine.Model.CommandSpec result = findCommandFor(option, sub.getCommandSpec());
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private CommandLine.Model.@Nullable CommandSpec findCommandFor(
        CommandLine.Model.PositionalParamSpec positional,
        CommandLine.Model.CommandSpec commandSpec
    ) {
        for (CommandLine.Model.PositionalParamSpec defined : commandSpec.positionalParameters()) {
            if (defined == positional) {
                return commandSpec;
            }
        }

        for (CommandLine sub : commandSpec.subcommands().values()) {
            CommandLine.Model.CommandSpec result = findCommandFor(positional, sub.getCommandSpec());
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private boolean isPicocliModelObject(Object obj) {
        return obj instanceof CommandLine.Model.CommandSpec
            || obj instanceof CommandLine.Model.OptionSpec
            || obj instanceof CommandLine.Model.PositionalParamSpec;
    }

    private void filterAndTrimMatchingPrefix(String prefix, List<Completions.Candidate> candidates) {
        Set<Completions.Candidate> replace = new HashSet<>();
        for (Completions.Candidate candidate : candidates) {
            String seq = candidate.getName();
            if (seq.startsWith(prefix)) {
                replace.add(new Completions.Candidate(
                    seq.subSequence(prefix.length(), seq.length()).toString(),
                    candidate.getDescription()
                ));
            }
        }

        candidates.clear();
        candidates.addAll(replace);
    }

    private void addCandidatesForArgsFollowing(
        @Nullable Object obj,
        Completions.Line line,
        List<Completions.Candidate> candidates,
        @Nullable Object command
    ) {
        if (obj instanceof CommandLine.Model.CommandSpec) {
            addCandidatesForArgsFollowing((CommandLine.Model.CommandSpec) obj, line, candidates, command);
        } else if (obj instanceof CommandLine.Model.OptionSpec) {
            addCandidatesForArgsFollowing((CommandLine.Model.OptionSpec) obj, line, candidates, command);
        } else if (obj instanceof CommandLine.Model.PositionalParamSpec) {
            addCandidatesForArgsFollowing((CommandLine.Model.PositionalParamSpec) obj, line, candidates, command);
        }
    }

    private void addCandidatesForArgsFollowing(
        CommandLine.Model.@Nullable CommandSpec commandSpec,
        Completions.Line line,
        List<Completions.Candidate> candidates,
        @Nullable Object command
    ) {
        if (commandSpec == null) {
            return;
        }

        addSubcommandsAndOptions(commandSpec, candidates);

        // Only suggest the first positional parameter here - no positional value has been provided
        // yet at this point, so the following positionals must not be offered until it is filled.
        for (CommandLine.Model.PositionalParamSpec positional : commandSpec.positionalParameters()) {
            if (positional.hidden()) {
                continue;
            } // #887 skip hidden subcommands

            if (positional.index().min() == 0) {
                addCandidatesForArgsFollowing(positional, line, candidates, command);
            }
        }
    }

    private void addNextPositional(
        CommandLine.Model.CommandSpec commandSpec,
        CommandLine.ParseResult parseResult,
        Completions.Line line,
        List<Completions.Candidate> candidates,
        @Nullable Object command
    ) {
        int filledSlots = matchedPositionalSlots(commandSpec, parseResult);
        for (CommandLine.Model.PositionalParamSpec positional : commandSpec.positionalParameters()) {
            if (positional.hidden()) {
                continue;
            } // #887 skip hidden positionals

            CommandLine.Range index = positional.index();
            if (filledSlots >= index.min() && filledSlots <= index.max()) {
                addCandidatesForArgsFollowing(positional, line, candidates, command);
            }
        }
    }

    private int matchedPositionalSlots(
        CommandLine.Model.CommandSpec commandSpec,
        CommandLine.ParseResult parseResult
    ) {
        Set<CommandLine.Model.PositionalParamSpec> owned = new HashSet<>(commandSpec.positionalParameters());
        int count = 0;
        for (Object matched : tentativeMatch(parseResult)) {
            if (matched instanceof CommandLine.Model.PositionalParamSpec && owned.contains(matched)) {
                count++;
            }
        }
        return count;
    }

    private void addSubcommandsAndOptions(
        CommandLine.Model.@Nullable CommandSpec commandSpec,
        List<Completions.Candidate> candidates
    ) {
        if (commandSpec == null) {
            return;
        }

        for (Map.Entry<String, CommandLine> entry : commandSpec.subcommands().entrySet()) {
            if (entry.getValue().getCommandSpec().usageMessage().hidden()) {
                continue;
            } // #887 skip hidden subcommands
            candidates.add(new Completions.Candidate(
                entry.getKey(),
                String.join(
                    " ",
                    entry.getValue().getCommandSpec().usageMessage().description()
                )
            ));
            for (String alias : entry.getValue().getCommandSpec().aliases()) {
                candidates.add(new Completions.Candidate(
                    alias,
                    String.join(
                        " ",
                        entry.getValue().getCommandSpec().usageMessage().description()
                    )
                ));
            }
        }

        commandSpec.optionsMap().forEach((option, spec) -> {
            candidates.add(new Completions.Candidate(option, String.join(" ", spec.description())));
        });
    }

    private void addCandidatesForArgsFollowing(
        CommandLine.Model.@Nullable OptionSpec optionSpec,
        Completions.Line line,
        List<Completions.Candidate> candidates,
        @Nullable Object command
    ) {
        if (optionSpec != null && !optionSpec.hidden()) {
            addCompletionCandidates(optionSpec.completionCandidates(), line, candidates, command);
        }
    }

    private void addCandidatesForArgsFollowing(
        CommandLine.Model.@Nullable PositionalParamSpec positionalSpec,
        Completions.Line line,
        List<Completions.Candidate> candidates,
        @Nullable Object command
    ) {
        if (positionalSpec != null && !positionalSpec.hidden()) {
            addCompletionCandidates(positionalSpec.completionCandidates(), line, candidates, command);
        }
    }

    private void addCompletionCandidates(
        @Nullable Iterable<String> completionCandidates,
        Completions.Line line,
        List<Completions.Candidate> candidates,
        @Nullable Object command
    ) {
        if (completionCandidates instanceof Completions) {
            for (Completions.Candidate candidate : ((Completions) completionCandidates).candidates(command, line)) {
                candidates.add(candidate);
            }
        } else if (completionCandidates != null) {
            for (String candidate : completionCandidates) {
                candidates.add(new Completions.Candidate(candidate, null));
            }
        }
    }

}
