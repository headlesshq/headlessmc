package io.github.headlesshq.headlessmc.platform.paper.api;

import java.util.Locale;

/**
 * Represents a Paper API release channel.
 */
public enum Channel {
    ALPHA, BETA, STABLE, RECOMMENDED, UNKNOWN;

    /**
     * Parses a Channel from the given name.
     * Returns {@link Channel#UNKNOWN} for unknown channels.
     *
     * @param name the name of the channel.
     * @return the parsed channel.
     */
    public static Channel of(String name) {
        try {
            return Channel.valueOf(name.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return Channel.UNKNOWN;
        }
    }

}
