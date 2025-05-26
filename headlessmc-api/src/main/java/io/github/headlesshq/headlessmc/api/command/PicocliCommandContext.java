package io.github.headlesshq.headlessmc.api.command;

import org.jetbrains.annotations.Unmodifiable;
import picocli.CommandLine;

import java.util.List;

public interface PicocliCommandContext extends CommandContext {
    int getExitCode();

    @Unmodifiable
    List<CommandLine> getHistory();

}
