package io.github.headlesshq.headlessmc.api.command;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class YesNoContext implements CommandContext {
    private static final Pattern YES = Pattern.compile("y+e*s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NO = Pattern.compile("n+o*", Pattern.CASE_INSENSITIVE);

    private final Consumer<Boolean> callback;

    @Override
    public void execute(String command) {
        executeArgs(command);
    }

    @Override
    public void executeArgs(String... args) throws CommandException {
        if (args.length == 0) {
            throw new CommandException("[Y/n] expected.");
        }

        if (YES.matcher(args[0]).matches()) {
            callback.accept(true);
        } else if (NO.matcher(args[0]).matches()) {
            callback.accept(false);
        } else {
            throw new CommandException("[Y/n] expected.");
        }
    }

    @Override
    public List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) {
        return new ArrayList<>(Arrays.asList(
                new Suggestion("y", null, true),
                new Suggestion("n", null, true))
        );
    }

    public static void input(CommandLineManager commandLineManager, Consumer<Boolean> callback) {
        CommandContext before = commandLineManager.getInteractiveContext();
        commandLineManager.setInteractiveContext(new YesNoContext(result -> {
            commandLineManager.setInteractiveContext(before);
            callback.accept(result);
        }));
    }

}
