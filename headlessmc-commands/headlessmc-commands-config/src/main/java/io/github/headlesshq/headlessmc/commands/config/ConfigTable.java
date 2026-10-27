package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.ConfigDescriptionService;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Prints config properties with their values and descriptions.
 * Properties without a value are shown as {@code -},
 * the descriptions of optional properties start with {@value #OPTIONAL}.
 */
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConfigTable {
    static final String OPTIONAL = "Optional: ";

    private final ConfigDescriptionService descriptionService;
    private final TableProvider tableProvider;

    /**
     * Writes a table of the given properties to the console.
     *
     * @param configService the service to get values from.
     * @param properties    the names of the properties to print.
     * @param console       the console to write the table to.
     */
    public void log(ConfigService configService, List<String> properties, Console console) {
        Set<String> optional = configService.getOptionalPropertyNames();
        tableProvider.<String>get()
            .withColumn("name", Function.identity())
            .withColumn("value", name -> configService.getConfig().getOptionalValue(name, String.class)
                .map(value -> SecretConfigValues.mask(name, value))
                .orElse("-"))
            .withColumn("description", name -> describe(name, optional.contains(name)))
            .addAll(properties)
            .log(console::write);
    }

    private String describe(String name, boolean optional) {
        String description = descriptionService.getDescription(name).orElse(null);
        if (!optional) {
            return description == null ? "-" : description;
        }

        return description == null ? "Optional" : OPTIONAL + description;
    }

}
