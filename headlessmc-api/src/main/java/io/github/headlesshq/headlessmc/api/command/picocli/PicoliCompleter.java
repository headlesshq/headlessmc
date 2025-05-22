package io.github.headlesshq.headlessmc.api.command.picocli;

class PicoliCompleter {
    /*public static int complete(CommandLine.Model.CommandSpec spec, String[] args, int argIndex, int positionInArg, int cursor, List<CharSequence> candidates) {
        if (spec == null)       { throw new NullPointerException("spec is null"); }
        if (args == null)       { throw new NullPointerException("args is null"); }
        if (candidates == null) { throw new NullPointerException("candidates list is null"); }
        if (argIndex == args.length) {
            String[] copy = new String[args.length + 1];
            System.arraycopy(args, 0, copy, 0, args.length);
            args = copy;
            args[argIndex] = "";
        }
        if (argIndex < 0      || argIndex >= args.length)                 { throw new IllegalArgumentException("Invalid argIndex " + argIndex + ": args array only has " + args.length + " elements."); }
        if (positionInArg < 0 || positionInArg > args[argIndex].length()) { throw new IllegalArgumentException("Invalid positionInArg " + positionInArg + ": args[" + argIndex + "] (" + args[argIndex] + ") only has " + args[argIndex].length() + " characters."); }

        String currentArg = args[argIndex];
        boolean reset = spec.parser().collectErrors();
        try {
            String committedPrefix = currentArg.substring(0, positionInArg);

            spec.parser().collectErrors(true);
            CommandLine parser = new CommandLine(spec);
            CommandLine.ParseResult parseResult = parser.parseArgs(args);
            if (argIndex >= parseResult.tentativeMatch.size()) {
                Object startPoint = findCompletionStartPoint(parseResult);
                addCandidatesForArgsFollowing(startPoint, candidates);
            } else {
                Object obj = parseResult.tentativeMatch.get(argIndex);
                if (obj instanceof CommandLine.Model.CommandSpec) { // subcommand
                    addCandidatesForArgsFollowing(((CommandLine.Model.CommandSpec) obj).parent(), candidates);

                } else if (obj instanceof CommandLine.Model.OptionSpec) { // option
                    int sep = currentArg.indexOf(spec.parser().separator());
                    if (sep < 0 || positionInArg < sep) { // no '=' or cursor before '='
                        addCandidatesForArgsFollowing(findCommandFor((CommandLine.Model.OptionSpec) obj, spec), candidates);
                    } else {
                        addCandidatesForArgsFollowing((CommandLine.Model.OptionSpec) obj, candidates);

                        int sepLength = spec.parser().separator().length();
                        if (positionInArg < sep + sepLength) {
                            int posInSeparator = positionInArg - sep;
                            String prefix = spec.parser().separator().substring(posInSeparator);
                            for (int i = 0; i < candidates.size(); i++) {
                                candidates.set(i, prefix + candidates.get(i));
                            }
                            committedPrefix = currentArg.substring(sep, positionInArg);
                        } else {
                            committedPrefix = currentArg.substring(sep + sepLength, positionInArg);
                        }
                    }

                } else if (obj instanceof CommandLine.Model.PositionalParamSpec) { // positional
                    //addCandidatesForArgsFollowing(obj, candidates);
                    addCandidatesForArgsFollowing(findCommandFor((CommandLine.Model.PositionalParamSpec) obj, spec), candidates);

                } else {
                    int i = argIndex - 1;
                    while (i > 0 && !isPicocliModelObject(parseResult.tentativeMatch.get(i))) {i--;}
                    if (i < 0) { return -1; }
                    addCandidatesForArgsFollowing(parseResult.tentativeMatch.get(i), candidates);
                }
            }
            filterAndTrimMatchingPrefix(committedPrefix, candidates);
            return candidates.isEmpty() ? -1 : cursor;
        } finally {
            spec.parser().collectErrors(reset);
        }
    }
    private static Object findCompletionStartPoint(CommandLine.ParseResult parseResult) {
        List<Object> tentativeMatches = parseResult.tentativeMatch;
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

    private static CommandLine.Model.CommandSpec findCommandFor(CommandLine.Model.ArgSpec arg, CommandLine.Model.CommandSpec cmd) {
        return (arg instanceof CommandLine.Model.OptionSpec) ? findCommandFor((CommandLine.Model.OptionSpec) arg, cmd) : findCommandFor((CommandLine.Model.PositionalParamSpec) arg, cmd);
    }
    private static CommandLine.Model.CommandSpec findCommandFor(CommandLine.Model.OptionSpec option, CommandLine.Model.CommandSpec commandSpec) {
        for (CommandLine.Model.OptionSpec defined : commandSpec.options()) {
            if (defined == option) { return commandSpec; }
        }
        for (CommandLine sub : commandSpec.subcommands().values()) {
            CommandLine.Model.CommandSpec result = findCommandFor(option, sub.getCommandSpec());
            if (result != null) { return result; }
        }
        return null;
    }
    private static CommandLine.Model.CommandSpec findCommandFor(CommandLine.Model.PositionalParamSpec positional, CommandLine.Model.CommandSpec commandSpec) {
        for (CommandLine.Model.PositionalParamSpec defined : commandSpec.positionalParameters()) {
            if (defined == positional) { return commandSpec; }
        }
        for (CommandLine sub : commandSpec.subcommands().values()) {
            CommandLine.Model.CommandSpec result = findCommandFor(positional, sub.getCommandSpec());
            if (result != null) { return result; }
        }
        return null;
    }
    private static boolean isPicocliModelObject(Object obj) {
        return obj instanceof CommandLine.Model.CommandSpec || obj instanceof CommandLine.Model.OptionSpec || obj instanceof CommandLine.Model.PositionalParamSpec;
    }

    private static void filterAndTrimMatchingPrefix(String prefix, List<CharSequence> candidates) {
        Set<CharSequence> replace = new HashSet<CharSequence>();
        for (CharSequence seq : candidates) {
            if (seq.toString().startsWith(prefix)) {
                replace.add(seq.subSequence(prefix.length(), seq.length()));
            }
        }
        candidates.clear();
        candidates.addAll(replace);
    }
    private static void addCandidatesForArgsFollowing(Object obj, List<CharSequence> candidates) {
        if (obj == null) { return; }
        if (obj instanceof CommandLine.Model.CommandSpec) {
            addCandidatesForArgsFollowing((CommandLine.Model.CommandSpec) obj, candidates);
        } else if (obj instanceof CommandLine.Model.OptionSpec) {
            addCandidatesForArgsFollowing((CommandLine.Model.OptionSpec) obj, candidates);
        } else if (obj instanceof CommandLine.Model.PositionalParamSpec) {
            addCandidatesForArgsFollowing((CommandLine.Model.PositionalParamSpec) obj, candidates);
        }
    }
    private static void addCandidatesForArgsFollowing(CommandLine.Model.CommandSpec commandSpec, List<CharSequence> candidates) {
        if (commandSpec == null) { return; }
        for (Map.Entry<String, CommandLine> entry : commandSpec.subcommands().entrySet()) {
            if (entry.getValue().getCommandSpec().usageMessage().hidden()) { continue; } // #887 skip hidden subcommands
            candidates.add(entry.getKey());
            candidates.addAll(Arrays.asList(entry.getValue().getCommandSpec().aliases()));
        }
        candidates.addAll(commandSpec.optionsMap().keySet());
        for (CommandLine.Model.PositionalParamSpec positional : commandSpec.positionalParameters()) {
            if (positional.hidden()) { continue; } // #887 skip hidden subcommands
            addCandidatesForArgsFollowing(positional, candidates);
        }
    }
    private static void addCandidatesForArgsFollowing(CommandLine.Model.OptionSpec optionSpec, List<CharSequence> candidates) {
        if (optionSpec != null && !optionSpec.hidden()) {
            addCompletionCandidates(optionSpec.completionCandidates(), candidates);
        }
    }
    private static void addCandidatesForArgsFollowing(CommandLine.Model.PositionalParamSpec positionalSpec, List<CharSequence> candidates) {
        if (positionalSpec != null && !positionalSpec.hidden()) {
            addCompletionCandidates(positionalSpec.completionCandidates(), candidates);
        }
    }
    private static void addCompletionCandidates(Iterable<String> completionCandidates, List<CharSequence> candidates) {
        if (completionCandidates != null) {
            for (String candidate : completionCandidates) { candidates.add(candidate); }
        }
    }*/

}
