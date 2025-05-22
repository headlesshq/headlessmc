package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.command.CommandException;

/**
 * @see PicocliCommandContext
 */
public interface CommandContext extends ProvidesSuggestions {
    void execute(String command) throws CommandException;

}
