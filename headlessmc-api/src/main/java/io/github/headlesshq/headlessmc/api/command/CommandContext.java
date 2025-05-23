package io.github.headlesshq.headlessmc.api.command;

/**
 * @see PicocliCommandContext
 */
public interface CommandContext extends ProvidesSuggestions {
    void execute(String command) throws CommandException;

}
