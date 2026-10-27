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
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import java.util.List;

@Getter
@Setter
@Named("headlessmc:commands:config:get")
@CommandLine.Command(
    name = "get",
    mixinStandardHelpOptions = true,
    description = "Shows a specific config property"
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class GetCommand implements Runnable, CachedConsole.Enabled {
    private final ConfigDescriptionService descriptionService;
    private final ConfigService configService;
    private final TableProvider tableProvider;
    private final Console console;

    @CommandLine.Parameters(
        paramLabel = "property",
        index = "0",
        arity = "1",
        description = "The property to set",
        completionCandidates = ConfigCompletions.class
    )
    private @Nullable String property;

    @Override
    public void run() {
        String property = this.property;
        if (property == null) {
            throw new IllegalArgumentException("Please specify a config property to get!");
        }

        ConfigTable.log(List.of(property), configService, descriptionService, tableProvider, console);
    }
}
