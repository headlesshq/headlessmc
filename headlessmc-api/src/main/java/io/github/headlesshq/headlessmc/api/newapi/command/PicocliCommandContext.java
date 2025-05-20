package io.github.headlesshq.headlessmc.api.newapi.command;

import picocli.CommandLine;

public interface PicocliCommandContext extends CommandContext {
    CommandLine getPicocli();

}
