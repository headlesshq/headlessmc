package io.github.headlesshq.headlessmc.commands.server;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

@Getter
@Setter
@Default
@Dependent
@Named("command:server")
@CommandLine.Command(
    name = "server",
    mixinStandardHelpOptions = true,
    description = "Manage servers.",
    subcommands = {
        AddCommand.class,
        ListCommand.class,
        RemoveCommand.class,
        ServerLaunchCommand.class,
        EulaCommand.class,
    }
)
@RequiredArgsConstructor
public class ServerCommand {

}
