package io.github.headlesshq.headlessmc.commands.config;

import java.util.List;
import java.util.Locale;

/**
 * Hides the values of config properties that hold secrets, like tokens or api keys,
 * so that the config commands do not print them.
 * Properties are recognized as secret by their name.
 */
final class SecretConfigValues {
    static final String MASK = "********";

    private static final List<String> SECRET_SUFFIXES = List.of(".token", ".api-key", ".password", ".secret");

    private SecretConfigValues() {
        throw new AssertionError();
    }

    /**
     * @param property the name of a config property.
     * @return {@code true} if the property holds a secret.
     */
    static boolean isSecret(String property) {
        String lowerCase = property.toLowerCase(Locale.ENGLISH);
        return SECRET_SUFFIXES.stream().anyMatch(lowerCase::endsWith);
    }

    /**
     * @param property the name of a config property.
     * @param value    the value of the property.
     * @return {@link #MASK} if the property holds a secret, otherwise the value.
     */
    static String mask(String property, String value) {
        return isSecret(property) && !value.isEmpty() ? MASK : value;
    }

}
