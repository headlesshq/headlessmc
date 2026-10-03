package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.ConfigDescriptionService;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.console.cache.CachedConsole;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Function;

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
    private final ConfigDescriptionService descriptionService;
    private final ConfigService configService;
    private final TableProvider tableProvider;
    private final Console console;

    @Override
    public void run() {
        List<String> properties = configService.getPropertyNames().stream()
            .filter(property -> property.startsWith("hmc."))
            .toList();

        tableProvider.<String>get()
            .withColumn("name", Function.identity())
            .withColumn("value", name -> configService.getConfig().getValue(name, String.class))
            .withColumn("description", name -> descriptionService.getDescription(name).orElse("-"))
            .addAll(properties)
            .log(console::write);
    }

}
