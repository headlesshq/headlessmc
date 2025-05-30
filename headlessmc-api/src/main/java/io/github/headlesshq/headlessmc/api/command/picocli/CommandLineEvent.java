package io.github.headlesshq.headlessmc.api.command.picocli;

import lombok.AllArgsConstructor;
import lombok.Getter;
import picocli.CommandLine;

@Getter
@AllArgsConstructor
public class CommandLineEvent {
    private CommandLine commandLine;

}
