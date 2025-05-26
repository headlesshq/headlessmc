package io.github.headlesshq.headlessmc.api.command;

import io.github.headlesshq.headlessmc.api.HeadlessMc;
import jakarta.enterprise.context.Dependent;
import picocli.CommandLine;

@Dependent
@CommandLine.Command(
    name = "headlessmc",
    version = HeadlessMc.NAME_VERSION,
    mixinStandardHelpOptions = true,
    description = "HeadlessMcCommand"
)
public class RootCommand {
    // TODO: this!
}
