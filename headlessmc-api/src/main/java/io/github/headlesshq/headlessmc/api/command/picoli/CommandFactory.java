package io.github.headlesshq.headlessmc.api.command.picoli;

import io.github.headlesshq.headlessmc.api.command.StateManager;
import picocli.CommandLine;

public class CommandFactory implements CommandLine.IFactory {
    private final StateManager stateManager = new StateManager();

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        return null;
    }

    // TODO: commands want to be injected with stuff
    // TODO: commands might hold some state

}
