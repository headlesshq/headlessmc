package io.github.headlesshq.headlessmc.api.command.picocli;

import picocli.CommandLine;

@FunctionalInterface
public interface PicocliConfigurator {
    CommandLine configure(CommandLine commandLine);

}
