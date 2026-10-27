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

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Named("headlessmc:commands:config:list")
@CommandLine.Command(
    name = "list",
    aliases = "ls",
    mixinStandardHelpOptions = true,
    description = "Shows configurable config properties"
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ListCommand implements Runnable, CachedConsole.Enabled {
    private final ConfigService configService;
    private final ConfigTable configTable;
    private final Console console;

    @CommandLine.Option(
        names = {"-a", "--all"},
        description = "Displays all config properties, not just HeadlessMc properties",
        defaultValue = "false"
    )
    private boolean all;

    // TODO: @CommandLine.Parameters
    private List<String> searchParameters = new ArrayList<>();

    @Override
    public void run() {
        List<String> properties = configService.getPropertyNames().stream()
            .filter(property -> all || property.startsWith("hmc."))
            .toList();

        configTable.log(configService, properties, console);
    }

}
