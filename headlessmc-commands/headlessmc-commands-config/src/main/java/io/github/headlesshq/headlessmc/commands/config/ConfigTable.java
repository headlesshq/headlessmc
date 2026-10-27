package io.github.headlesshq.headlessmc.commands.config;

import io.github.headlesshq.headlessmc.config.ConfigDescriptionService;
import io.github.headlesshq.headlessmc.config.ConfigService;
import io.github.headlesshq.headlessmc.console.Console;
import io.github.headlesshq.headlessmc.console.format.TableProvider;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Prints config properties with their values and descriptions.
 * Properties without a value are shown as {@code -},
 * the descriptions of optional properties start with {@value #OPTIONAL}.
 */
final class ConfigTable {
    static final String OPTIONAL = "Optional: ";

    private ConfigTable() {
        throw new AssertionError();
    }

    /**
     * Writes a table of the given properties to the console.
     *
     * @param properties         the names of the properties to print.
     * @param configService      the service to get values from.
     * @param descriptionService the service to get descriptions from.
     * @param tableProvider      provides the table.
     * @param console            the console to write the table to.
     */
    static void log(
        List<String> properties,
        ConfigService configService,
        ConfigDescriptionService descriptionService,
        TableProvider tableProvider,
        Console console
    ) {
        Set<String> optional = configService.getOptionalPropertyNames();
        tableProvider.<String>get()
            .withColumn("name", Function.identity())
            .withColumn("value", name -> configService.getConfig().getOptionalValue(name, String.class)
                .map(value -> SecretConfigValues.mask(name, value))
                .orElse("-"))
            .withColumn("description", name -> describe(name, optional.contains(name), descriptionService))
            .addAll(properties)
            .log(console::write);
    }

    private static String describe(String name, boolean optional, ConfigDescriptionService descriptionService) {
        String description = descriptionService.getDescription(name).orElse(null);
        if (!optional) {
            return description == null ? "-" : description;
        }

        return description == null ? "Optional" : OPTIONAL + description;
    }

}
