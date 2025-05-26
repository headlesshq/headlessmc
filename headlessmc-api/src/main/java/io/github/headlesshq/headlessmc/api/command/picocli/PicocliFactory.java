package io.github.headlesshq.headlessmc.api.command.picocli;

import picocli.CommandLine;

@FunctionalInterface
public interface PicocliFactory {
    CommandLine create();

}
