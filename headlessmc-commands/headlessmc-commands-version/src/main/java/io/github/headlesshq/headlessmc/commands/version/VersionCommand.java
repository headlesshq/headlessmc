package io.github.headlesshq.headlessmc.commands.version;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

@Getter
@Setter
@Default
@Dependent
@Named("command:version")
@CommandLine.Command(
    name = "version",
    mixinStandardHelpOptions = true,
    description = "Manage versions.",
    subcommands = {
        RemoveCommand.class,
        InstallCommand.class,
        ListCommand.class
    }
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class VersionCommand implements Runnable, CachedConsole.Enabled {
    private final ListCommand listCommand;

    @Override
    public void run() {
        listCommand.run();
    }

}
