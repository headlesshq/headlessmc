package io.github.headlesshq.headlessmc.api.command;

import picocli.CommandLine;

public interface PicocliCommandContext extends CommandContext {
    CommandLine getPicocli();

    int getExitCode();

}
