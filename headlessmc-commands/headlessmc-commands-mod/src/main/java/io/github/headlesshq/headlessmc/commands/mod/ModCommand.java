package io.github.headlesshq.headlessmc.commands.mod;

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
@Named("command:mod")
@CommandLine.Command(
    name = "mod",
    mixinStandardHelpOptions = true,
    description = "Manage mods.",
    subcommands = {
        AddCommand.class,
        ListCommand.class,
        RemoveCommand.class,
        SearchCommand.class,
        WorldsCommand.class
    }
)
@RequiredArgsConstructor
public class ModCommand {

}
