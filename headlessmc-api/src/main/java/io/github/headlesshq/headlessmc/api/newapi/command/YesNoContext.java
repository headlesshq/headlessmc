package io.github.headlesshq.headlessmc.api.newapi.command;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
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
        if (YES.matcher(command).matches()) {
            callback.accept(true);
        } else if (NO.matcher(command).matches()) {
            callback.accept(false);
        } else {
            throw new CommandException("[Y/n] expected.");
        }
    }

    @Override
    public List<Suggestion> getSuggestions(String line, int cursor) {
        return new ArrayList<>();
    }

    @Override
    public List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) {
        return new ArrayList<>();
    }

    public static void input(CommandLineManager commandLineManager, Consumer<Boolean> callback) {
        CommandContext before = commandLineManager.getInteractiveContext();
        commandLineManager.setInteractiveContext(new YesNoContext(result -> {
            commandLineManager.setInteractiveContext(before);
            callback.accept(result);
        }));
    }

}
