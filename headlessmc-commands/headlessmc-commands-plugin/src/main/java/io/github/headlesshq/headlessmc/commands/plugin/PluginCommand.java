package io.github.headlesshq.headlessmc.commands.plugin;

import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;
import picocli.CommandLine;

@Default
@Dependent
@Named("command:plugin")
@CommandLine.Command(
    name = "plugin",
    aliases = "plugins",
    mixinStandardHelpOptions = true,
    description = "Manage HeadlessMc plugins.",
    subcommands = {
        ListCommand.class,
        CompileCommand.class
    }
)
public class PluginCommand implements CachedConsole.Enabled {

}
