package io.github.headlesshq.headlessmc.api.classloading;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.command.CommandContext;
import io.github.headlesshq.headlessmc.api.command.Suggestion;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class RemoteApplicationImpl implements RemoteApplication {
    private final Application application;

    @Override
    public void sendCommand(String command) throws CommandException {
        getCommandContext().execute(command);
    }

    @Override
    public List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) {
        return getCommandContext().getSuggestions(argIndex, positionInArg, cursor, args);
    }

    private CommandContext getCommandContext() {
        CommandContext commandContext = application.getCommandLine().getInteractiveContext();
        if (commandContext == null) {
            commandContext = application.getCommandLine().getContext();
        }

        return commandContext;
    }

}
