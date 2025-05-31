package io.github.headlesshq.headlessmc.launcher;

import io.github.headlesshq.headlessmc.api.command.CommandContext;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.Suggestion;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

@Getter
@Setter
public class MockedCommandContext implements CommandContext {
    private Consumer<String> callback;
    private String command;

    @Override
    public void execute(String command) {
        this.command = command;
        if (callback != null) {
            callback.accept(command);
        }
    }

    @Override
    public void executeArgs(String... command) throws CommandException {

    }

    @Override
    public Iterator<Command> iterator() {
        return Collections.emptyIterator();
    }

    public String checkAndReset() {
        String cmd = command;
        command = null;
        return cmd;
    }

    @Override
    public List<Suggestion> getSuggestions(int argIndex, int positionInArg, int cursor, String... args) {
        return List.of();
    }
}
