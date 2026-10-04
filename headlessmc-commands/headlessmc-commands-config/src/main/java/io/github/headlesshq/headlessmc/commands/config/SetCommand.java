package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.ConfigDescriptionService;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.ConsoleExtensions;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

@Getter
@Setter
@Named("headlessmc:commands:config:set")
@CommandLine.Command(
    name = "set",
    mixinStandardHelpOptions = true,
    description = "Configures a config property"
)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
// specifically not CachedConsole.Enabled as jline configuration could change
public class SetCommand implements Runnable {
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

    @CommandLine.Parameters(
        paramLabel = "value",
        index = "1",
        arity = "0..1",
        description = "The value to set"
    )
    private @Nullable String value;

    @CommandLine.Option(
        names = "--temp",
        description = "Only use the value until HeadlessMc exits, do not write it to the config file."
    )
    private boolean temp;

    @Override
    public void run() {
        if (property == null) {
            throw new IllegalArgumentException("Please specify a config property to set.");
        }

        String value = this.value;
        if (value == null) {
            ConsoleExtensions extensions = console.extensions()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Please specify the value to set " + property + " to, or use another terminal."
                ));

            String before = configService.getConfig().getValue(property, String.class);
            value = extensions.edit(before);
        }

        configService.set(property, value, !temp);
        console.write("Set " + property + " to " + value + (temp ? " temporarily" : ""));
    }
    
}
