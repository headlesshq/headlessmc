package io.github.headlesshq.headlessmc.api.newapi.command;

import io.github.headlesshq.headlessmc.api.command.CommandException;

import java.util.List;

public interface CommandContext {
    void execute(String command) throws CommandException;

    List<Suggestion> getSuggestions(String line, int cursor);

    List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args);

}
