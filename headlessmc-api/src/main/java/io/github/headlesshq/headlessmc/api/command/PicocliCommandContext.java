package io.github.headlesshq.headlessmc.api.command;

import lombok.Data;
import org.jetbrains.annotations.Unmodifiable;
import picocli.CommandLine;

import java.util.Deque;

public interface PicocliCommandContext extends CommandContext {
    @Unmodifiable
    Deque<PastExecution> getHistory();

    @Data
    class PastExecution {
        private final CommandLine commandLine;
        private final int exitCode;
    }

}
