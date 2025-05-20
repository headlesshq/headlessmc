package io.github.headlesshq.headlessmc.launcher;

import lombok.Getter;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.command.CommandContext;

import java.util.Collections;
import java.util.Iterator;
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
    public Iterator<Command> iterator() {
        return Collections.emptyIterator();
    }

    public String checkAndReset() {
        String cmd = command;
        command = null;
        return cmd;
    }

}
