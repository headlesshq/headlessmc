package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.logging.StdIO;
import org.jetbrains.annotations.Nullable;

public interface CommandLineManager {
    StdIO getStdIO();

    PicocliCommandContext getContext();

    @Nullable CommandContext getInteractiveContext();

    void setInteractiveContext(@Nullable CommandContext context);

    CommandLineReader getReader();

    default CommandContext getActiveContext() {
        CommandContext context = getInteractiveContext();
        if (context == null) {
            context = getContext();
        }

        return context;
    }

}
