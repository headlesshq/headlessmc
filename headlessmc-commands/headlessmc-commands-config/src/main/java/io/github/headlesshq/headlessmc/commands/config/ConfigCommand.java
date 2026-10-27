package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.Console;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.util.List;

@Getter
@Setter
@Named("headlessmc:commands:config")
@CommandLine.Command(
    name = "config",
    mixinStandardHelpOptions = true,
    description = "Configures HeadlessMc",
    subcommands = {
        ListCommand.class,
        SetCommand.class,
        GetCommand.class
    }
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConfigCommand implements Runnable, CachedConsole.Enabled {
    private final ConfigService configService;
    private final ConfigTable configTable;
    private final Console console;

    @Override
    public void run() {
        List<String> properties = configService.getPropertyNames().stream()
            .filter(property -> property.startsWith("hmc."))
            .toList();

        configTable.log(configService, properties, console);
    }

}
