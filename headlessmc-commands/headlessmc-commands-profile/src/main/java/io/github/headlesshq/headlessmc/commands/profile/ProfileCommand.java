package io.github.headlesshq.headlessmc.commands.profile;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
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
@Named("command:profile")
@CommandLine.Command(
    name = "profile",
    mixinStandardHelpOptions = true,
    description = "Manage launch profiles.",
    subcommands = {
        AddCommand.class,
        ListCommand.class,
        RemoveCommand.class,
        EditCommand.class,
        ProfileLaunchCommand.class
    }
)
@RequiredArgsConstructor
public class ProfileCommand implements CachedConsole.Enabled {

}
